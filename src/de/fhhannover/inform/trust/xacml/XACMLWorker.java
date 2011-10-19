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

/**
 ** License for components that are based on or use parts of the Sun XACML
 ** implementation. (http://sunxacml.sourceforge.net/)
 **/
/*
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 */

package de.fhhannover.inform.trust.xacml;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.net.Socket;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * This class is responsible for the XACML evaluation. It will be instantiated
 * for every incoming XACML socket connection, and handles the incoming network
 * data, the parsing of the data into a XML document, the evaluation by the
 * NetworkPDP and sending the XACML response back.
 * 
 * 
 * @author Bastian Hellmann
 */
public class XACMLWorker extends Thread {

	private static Logger logger = Logger.getLogger(XACMLWorker.class);

	private NetworkPDP pdp;

	private Socket socket;

	private int number;

	/**
	 * Creates a new XACMLWorker.
	 * 
	 * @param pdp
	 *            reference to the PDP which does the evaluation
	 * @param socket
	 *            the socket to use for this XACML evaluation; request and
	 *            response are transmitted via this socket
	 * @param number
	 *            the number for this thread instance
	 */
	public XACMLWorker(NetworkPDP pdp, Socket socket, int number) {
		super("XACMLWorker");
		this.pdp = pdp;
		this.socket = socket;
		this.number = number;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Thread#run()
	 */
	/**
	 * This method does 1. reading the data from the socket to a String, until a
	 * </Request> string is found 2. parse that String to a XML document 3.
	 * evaluate that XML document via the PDP 4. convert the response into a
	 * String 5. send that String back to the client
	 */
	public void run() {
		try {
			PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
			BufferedReader in = new BufferedReader(new InputStreamReader(
					socket.getInputStream()));

			logger.info("worker #" + this.number
					+ ": Waiting for request from client "
					+ socket.getInetAddress().getHostAddress() + " ...");

			/**
			 * Receiving request.
			 */
			StringBuilder sb = new StringBuilder();
			String currentLine;

			int line = 1;
			while ((currentLine = in.readLine()) != null) {
				sb.append(currentLine);
				sb.append("\n");
				logger.trace("line #" + line + ": " + currentLine);

				if (currentLine.contains("</Request>")) {
					logger.info("Request finished after " + line + " lines");
					break;
				}

				line++;
			}

			String request = sb.toString();

			logger.info("worker #" + this.number
					+ ": Receving request from client "
					+ socket.getInetAddress().getHostAddress());
			logger.debug("worker #" + this.number + ": Request: " + request);

			Document requestDoc = parseXMLString(request);
			Node requestRoot = requestDoc.getDocumentElement();

			/**
			 * Evaluate the request.
			 */
			logger.info("worker #" + this.number
					+ ": Evaluating the request from "
					+ socket.getInetAddress().getHostAddress());
			logger.debug("worker #" + this.number
					+ ": Document root of request is <"
					+ requestRoot.toString() + ">");

			try {
				ResponseCtx responseCtx = this.pdp.evaluate(requestRoot);

				if (responseCtx != null) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					responseCtx.encode(baos, new Indenter());
					String response = baos.toString();

					/**
					 * Send the response back to the client.
					 */
					out.println(response);
					logger.info("worker #" + this.number
							+ ": Sending response to client "
							+ socket.getInetAddress().getHostAddress());
					logger.debug("worker #" + this.number + ": Response: "
							+ response);
				} else {
					logger.error("worker #" + this.number
							+ ": Request could not be evaluated.");
				}
			} catch (NullPointerException e) {
				logger.error("worker #" + this.number
						+ ": Request could not be evaluated.");
			}

			out.close();
			logger.debug("worker #" + this.number
					+ ": Closing outgoing connection.");

			in.close();
			logger.debug("worker #" + this.number
					+ ": Closing incoming connection.");

			socket.close();
			logger.debug("worker #" + this.number + ": Closing socket.");

			logger.info("worker #" + this.number
					+ ": Shutting down XACML worker unit.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParsingException e) {
			e.printStackTrace();
		}
	}

	/**
	 * A helper method, which converts a given String to a XML document.
	 * 
	 * @param request
	 *            the String to convert
	 * @return a XML document of the given String
	 */
	private Document parseXMLString(String request) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
			InputSource is = new InputSource();
			is.setCharacterStream(new StringReader(request));
			Document doc = db.parse(is);

			return doc;
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return null;
	}
}
