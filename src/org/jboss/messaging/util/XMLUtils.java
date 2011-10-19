/**
 * JBoss, Home of Professional Open Source
 *
 * Distributable under LGPL license.
 * See terms of license at gnu.org.
 */

package org.jboss.messaging.util;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Changes (line 55 and 76) made by tnc@fhh team
 * https://trust.inform.fh-hannover.de
 */

/**
 * @author <a href="mailto:ovidiu@feodorov.com">Ovidiu Feodorov</a>
 * @version <tt>$Revision: 3282 $</tt>
 * $Id: XMLUtil.java 3282 2007-11-01 15:32:29Z timfox $
 */
public class XMLUtils {
  public static String elementToString(Node n) {

    String name = n.getNodeName();

    short type = n.getNodeType();

    if (Node.CDATA_SECTION_NODE == type) {
      return "<![CDATA[" + n.getNodeValue() + "]]&gt;";
    }

    if (name.startsWith("#")) {
      return "";
    }

    StringBuilder sb = new StringBuilder();
    sb.append('<').append(name);

    NamedNodeMap attrs = n.getAttributes();
    if (attrs != null) {
      for (int i = 0; i < attrs.getLength(); i++) {
        Node attr = attrs.item(i);
        sb.append(' ').append(attr.getNodeName()).append("=\"").append(attr.getNodeValue()).append(
            "\"");
      }
    }

    String textContent = null;
    NodeList children = n.getChildNodes();

    if (children.getLength() == 0) {
//      if ((textContent = XMLUtil.getTextContent(n)) != null && !"".equals(textContent)) {
      if ((textContent = n.getTextContent()) != null && !"".equals(textContent)) {
        sb.append(textContent).append("</").append(name).append('>');

      } else {
        sb.append("/>");
      }
    } else {
      sb.append('>');
      boolean hasValidChildren = false;
      for (int i = 0; i < children.getLength(); i++) {
        String childToString = elementToString(children.item(i));
        if (!"".equals(childToString)) {
          sb.append('\n').append(childToString);
          hasValidChildren = true;
        }
      }
      if (hasValidChildren) {
          sb.append('\n');
      }

//      if (!hasValidChildren && ((textContent = XMLUtil.getTextContent(n)) != null)) {
      if (!hasValidChildren && ((textContent = n.getTextContent()) != null)) {
        sb.append(textContent);
      }

      sb.append("</").append(name).append('>');
    }

    return sb.toString();
  }
}