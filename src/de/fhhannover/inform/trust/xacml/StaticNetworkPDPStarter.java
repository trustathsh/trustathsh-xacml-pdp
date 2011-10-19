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

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import com.sun.xacml.UnknownIdentifierException;

/**
 * Start class for the Trust@FHH XACML PDP. Uses the {@link StaticNetworkPDP},
 * which means that policies are only read on startup and changes are not
 * reflected during runtime.
 * 
 * @author Bastian Hellmann
 */
public class StaticNetworkPDPStarter {

	private static Logger logger = Logger
			.getLogger(StaticNetworkPDPStarter.class);

	/**
	 * Main-method. Starts the XACML PDP.
	 * 
	 * @param args
	 *            command-line arguments
	 * @throws Exception
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");

		if (args.length < 2) {
			System.err.println("Wrong number of arguments");
			System.err.println("Usage:");
			System.err
					.println("StaticNetworkPDPStarter <port> <policy file #1> <policy file #2> ... <policy file #n>");
			System.err
					.println("or                      <port> <filename postfix, e.g. '.xml'> <path to policy files (must end with defined filename postfix')>");
			System.exit(1);
		}

		int port = Integer.parseInt(args[0]);
		String[] policyFiles = new String[args.length - 1];

		logger.info("Trust@FHH XACML PDP with static policy handling (only loading during startup) starts.");

		if (args.length == 3 && new File(args[2]).isDirectory()) {
			File policyDirectory = new File(args[2]);
			final String filenamePostfix = args[1].replace(".", "");
			logger.info("Loading all files ending with '." + filenamePostfix
					+ "' from directory " + policyDirectory);
			policyFiles = policyDirectory.list(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					if (name.toLowerCase().endsWith(filenamePostfix)) {
						return true;
					} else {
						return false;
					}
				}
			});

			for (int i = 0; i < policyFiles.length; i++) {
				try {
					policyFiles[i] = policyDirectory.getCanonicalPath() + "/"
							+ policyFiles[i];
				} catch (IOException e) {
					logger.error("'IOException' at loading file: "
							+ e.getMessage());
				}
			}
		} else {
			logger.info("Loading all given policy files");
			for (int i = 1; i < args.length; i++) {
				policyFiles[i - 1] = args[i];
			}
		}

		for (int i = 0; i < policyFiles.length; i++) {
			logger.info("Policy file #" + (i + 1) + "/" + policyFiles.length
					+ ": " + policyFiles[i]);
		}

		NetworkPDP pdp;
		try {
			pdp = new StaticNetworkPDP(policyFiles);

			NetworkPDPDispatcher dispatcher;
			dispatcher = new NetworkPDPDispatcher(pdp, port);

			logger.info("Dispatcher is started.");

			dispatcher.runServer();
		} catch (URISyntaxException e) {
			logger.error("'URISyntaxException' during initialization of StaticNetworkPDP: "
					+ e.getMessage());
			System.exit(1);
		} catch (UnknownIdentifierException e) {
			logger.error("'UnknownIdentifierException' during initialization of StaticNetworkPDP: "
					+ e.getMessage());
			System.exit(1);
		}

	}
}
