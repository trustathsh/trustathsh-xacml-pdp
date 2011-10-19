/*
 * =====================================================
 *  _____                _     ____  _____ _   _ _   _ 
 * |_   _|_ __ _   _ ___| |_  / __ \|  ___| | | | | | |
 *   | | | '__| | | / __| __|/ / _` | |_  | |_| | |_| |
 *   | | | |  | |_| \__ \ |_| | (_| |  _| |  _  |  _  |
 *   |_| |_|   \__,_|___/\__|\ \__,_|_|   |_| |_|_| |_|
 *                            \____/                   
 *
 * =====================================================
 * 
 * Copyright (C) 2011, Fachhochschule Hannover
 * (University of Applied Sciences and Arts)
 * Ricklinger Stadtweg 118, 30459 Hannover, Germany
 *  
 * http://trust.inform.fh-hannover.de/
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 		http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 * 
 */

package de.fhhannover.inform.trust.xacml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class starts a XACML AR that will send a given XACML request to a
 * specified XACML PDP.
 * 
 * @author Bastian Hellmann
 */
public class NetworkAR {

	private static Logger logger = Logger.getLogger(NetworkAR.class);

	/**
	 * The Main-method of the NetworkAR. Reads in the command line arguments,
	 * tries to open a socket to the XACML PDP and sends the given XACML
	 * request. Then awaits an XACML response and prints it.
	 * 
	 * @param args
	 *            host-address of the XACML PDP, port of the XACML PDP and the
	 *            file with the XACML Request
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");

		String host = "";
		int port = 0;
		String requestFile = "";

		if (args.length != 3) {
			System.err.println("Wrong number of arguments");
			System.err.println("Usage:");
			System.err
					.println("NetworkAR <host-address> <port> <request-file>");
			System.exit(1);
		} else {
			host = args[0];
			port = Integer.parseInt(args[1]);
			requestFile = args[2];
		}

		logger.info("Trust@FHH XACML AR starts.");

		Socket socket = null;
		PrintWriter out = null;
		BufferedReader in = null;

		try {
			logger.info("Opening socket on " + host + " at port " + port);
			socket = new Socket(host, port);
			out = new PrintWriter(socket.getOutputStream(), true);
			in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			String request = readFile(requestFile);

			/**
			 * Send data to server
			 */
			logger.info("Sending request to server " + host);
			logger.debug("Request: " + request);

			out.println(request);

			/**
			 * Receive response from server
			 */
			StringBuilder sb = new StringBuilder();
			String currentLine;

			logger.info("Waiting for response from server " + host + " ...");

			int line = 1;
			while ((currentLine = in.readLine()) != null) {
				sb.append(currentLine);
				sb.append("\n");
				logger.trace("#" + line + ": " + currentLine);

				if (currentLine.equals("</Response>")) {
					logger.info("Response finished after " + line + " lines");
					break;
				}

				line++;
			}

			String response = sb.toString();

			logger.info("Receiving response from server " + host);
			logger.debug("Response: " + response);

			out.close();
			logger.info("Closing outgoing connection.");

			in.close();
			logger.info("Closing incoming connection.");

			socket.close();
			logger.info("Closing socket.");

			logger.info("Shutting down client.");
		} catch (UnknownHostException e) {
			logger.error("Don't know about host: " + host);
			System.exit(1);
		} catch (IOException e) {
			logger.error("Couldn't get I/O for " + "the connection to: " + host);
			System.exit(1);
		}
	}

	/**
	 * Helper method that reads in a file and stores it as a String.
	 * 
	 * @param filename
	 *            the file to read in
	 * @return content of that file as a String
	 */
	private static String readFile(String filename) {
		StringBuilder sb = new StringBuilder();

		try {
			BufferedReader br = new BufferedReader(new FileReader(new File(
					filename)));
			String currentLine;

			while ((currentLine = br.readLine()) != null) {
				sb.append(currentLine);
				sb.append("\n");
			}

			br.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return sb.toString();
	}
}
