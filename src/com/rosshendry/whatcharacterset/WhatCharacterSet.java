package com.rosshendry.whatcharacterset;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.SortedMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

public class WhatCharacterSet {

	/**
	 * An application to help find out what character set a string is being
	 * interpreted as after it's been stored in a database.
	 * 
	 * Run with -h for all the options available.
	 * 
	 * The best bet is to use "select hex( col )..." in mysql to get the byte
	 * string, then feed that into this application. It will use interpret that
	 * string using a variety of character sets to see what they come out as. The
	 * -f option allows to you narrow it down by supplying the the string that
	 * your application is outputting.
	 * 
	 * For example: "<command> -b 61646D696E" will loop through all available
	 * character sets interpreting those hex values as individual bytes.
	 * 
	 * Example 2: "<command> -s Ríkarðsdóttir -f "RÃ­karÃ°sdÃ³ttir"" tries to find
	 * which character sets mangle the input string to match the -f value. Note
	 * the quotes around the value to find.
	 * 
	 * @param args
	 */
	public static void main( String[] args ) throws UnsupportedEncodingException, DecoderException {
		Options options = new Options();
		options.addOption( "b", true, "bytes to decode" );
		options.addOption( "s", true, "string to reinterpret" );
		options.addOption( "c", "character-set", true, "character set to decode as" );
		options.addOption( "f", true, "decoded string to find" );
		options.addOption( "h", false, "display this help" );

		CommandLineParser parser = new GnuParser();
		CommandLine line = null;
		try {
			line = parser.parse( options, args );
		} catch (ParseException e) {
			System.out.println( e.getMessage() );
		}

		if ( line.hasOption( "h" ) ) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "WhatCharacterSet", options );
			System.exit( 0 );
		}

		Hex hex = new Hex();
		byte[] byteStream = new byte[0];

		if ( line.hasOption( "b" ) ) {
			String byteString = line.getOptionValue( "b" );
			System.out.println( "Got a byte string of " + byteString );

			try {
				byteStream = (byte[]) hex.decode( byteString );
			} catch (DecoderException e) {
				System.out.println( e.getMessage() );
			}

		} else if ( line.hasOption( "s" ) ) {
			String optionValue = line.getOptionValue( "s" );
			byteStream = hex.decode( hex.encode( optionValue.getBytes( "UTF-8" ) ) );

			System.out.println( String.format( "The string (%s) has %d characters.", optionValue, optionValue.length() ) );
			System.out.println( "That consists of " + byteStream.length + " bytes." );
			System.out.println( "Hex stream looks like " + Hex.encodeHexString( byteStream ) );

		} else {
			System.out.println( "No string supplied" );
			System.exit( 1 );
		}

		if ( line.hasOption( "c" ) ) {
			String cs = line.getOptionValue( "c" );
			// The user wants to decode as a specific character set
			if ( !Charset.isSupported( line.getOptionValue( "c" ) ) ) {
				System.out.println( String.format( "Character set %s is unsupported", cs ) );
				System.exit( 1 );
			}

			String outputString = new String( byteStream, Charset.forName( cs ) );
			System.out.println( String.format( "Interpreting as %s returned %s", cs, outputString ) );

		} else {
			SortedMap<String, Charset> availableCharacterSets = Charset.availableCharsets();
			String outputString = "";

			for ( Map.Entry<String, Charset> entry : availableCharacterSets.entrySet() ) {
				outputString = new String( byteStream, entry.getValue() );

				if ( line.hasOption( "f" ) ) {
					if ( outputString.equals( line.getOptionValue( "f" ) ) ) {
						System.out.println( String.format( "Interpreting as %s returned %s", entry.getKey(), outputString ) );
					}
				} else {
					System.out.println( String.format( "Interpreting as %s returned %s", entry.getKey(), outputString ) );
				}
			}
		}
	}

}
