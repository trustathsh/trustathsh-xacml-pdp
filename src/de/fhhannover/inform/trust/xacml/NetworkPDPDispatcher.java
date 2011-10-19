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

import java.io.IOException;
import java.net.ServerSocket;

import org.apache.log4j.Logger;

/**
 * This class handles incoming network connections. Every incoming connection
 * will be processed by an own {@link XACMLWorker}-instance,
 * 
 * @author Bastian Hellmann
 */
public class NetworkPDPDispatcher { // extends SimplePDP {

	private static Logger logger = Logger.getLogger(NetworkPDPDispatcher.class);

	private NetworkPDP pdp;

	private int port;

	private final int DEFAULT_PORT = 12345;

	/**
	 * Creates a new despatcher with the default port.
	 * 
	 * @param pdp
	 *            NetworkPDP that will handle the evaulation
	 */
	public NetworkPDPDispatcher(NetworkPDP pdp) {
		super();
		this.pdp = pdp;
		this.port = DEFAULT_PORT;
	}

	/**
	 * Creates a new despatcher with a given port.
	 * 
	 * @param pdp
	 *            NetworkPDP that will handle the evaulation
	 * @param port
	 *            the port, the server listens on
	 */
	public NetworkPDPDispatcher(NetworkPDP pdp, int port) {
		super();
		this.pdp = pdp;
		this.port = port;
	}

	/**
	 * Starts the server. It will listen on the specifed port and handles
	 * incoming connections to newly created {@link XACMLWorker}-instances.
	 */
	public void runServer() {
		boolean listening = true;
		ServerSocket serverSocket = null;

		try {
			serverSocket = new ServerSocket(this.port);
			logger.info("Server listening on port: " + this.port);

			int number = 1;
			while (listening) {
				new XACMLWorker(this.pdp, serverSocket.accept(), number)
						.start();
				logger.info("XACMLWorker #" + number + " starts.");
				number++;
			}

			serverSocket.close();
		} catch (IOException e) {
			logger.error("Could not listen on port: " + this.port);
			System.exit(1);
		}
	}
}
