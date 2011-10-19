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

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

/**
 * This class starts a XACML PDP that uses the {@link DynamicFilePolicyModule}
 * that will refresh its policies after a given time. It can recognize changes
 * on existing policies as well as newly added policies or policies, that were
 * deleted and therefore shall not be used further.
 * 
 * @author Bastian Hellmann
 */
public class DynamicNetworkPDPStarter {

	private static Logger logger = Logger
			.getLogger(StaticNetworkPDPStarter.class);

	/**
	 * Starts the Trust XACML PDP.
	 * 
	 * @param args
	 *            expects four parameters: 1. port, 2. refresh time for
	 *            policies, 3. filename extension for policy files and 4. path
	 *            to policy files to use
	 */
	public static void main(String[] args) {
		PropertyConfigurator.configure("log4j.properties");

		if (args.length != 4) {
			System.err.println("Wrong number of arguments");
			System.err.println("Usage:");
			System.err
					.println("DynamicNetworkPDPStarter <port> <refresh time for policies> <filename postfix, e.g. '.xml'> <path to policy files (must end with defined filename postfix')>");
			System.exit(1);
		} else {
			logger.info("Trust@FHH XACML PDP with (dynamic policy updating) starts.");

			int port = Integer.parseInt(args[0]);
			long refreshTime = Long.parseLong(args[1]);
			String filenamePostfix = args[2].replace(".", "");
			String directoryPath = args[3];

			File policyDirectory = new File(directoryPath);
			if (policyDirectory.isDirectory()) {
				logger.info("Loading all files ending with '."
						+ filenamePostfix + "' from directory "
						+ policyDirectory);

				DynamicNetworkPDP pdp = new DynamicNetworkPDP(policyDirectory,
						filenamePostfix, refreshTime);

				NetworkPDPDispatcher dispatcher;
				dispatcher = new NetworkPDPDispatcher(pdp, port);

				logger.info("Dispatcher is started.");

				dispatcher.runServer();
			} else {
				logger.error("Could'nt load directory: " + directoryPath);
				System.exit(1);
			}
		}
	}
}
