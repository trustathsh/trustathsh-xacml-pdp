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

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * This interface defines a NetworkPDP. The only needed method is the
 * evaluateNode method.
 * 
 * @author Bastian Hellmann
 */
public interface NetworkPDP {

	/**
	 * Evaluates the given request and returns the Response that the PDP will
	 * hand back to the PEP.
	 * 
	 * @param requestRoot
	 *            the root-node of the Request
	 * 
	 * @return the result of the evaluation
	 * 
	 * @throws IOException
	 *             if there is a problem accessing the file
	 * @throws ParsingException
	 *             if the Request is invalid
	 */
	public ResponseCtx evaluate(Node requestRoot) throws IOException,
			ParsingException;

}