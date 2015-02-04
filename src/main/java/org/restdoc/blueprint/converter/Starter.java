package org.restdoc.blueprint.converter;

import java.io.FileWriter;
import java.io.IOException;

import org.apache.commons.lang.CharEncoding;
import org.apache.http.HttpResponse;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.restdoc.api.RestDoc;
import org.restdoc.api.util.RestDocParser;

import de.taimos.httputils.WS;

/**
 * Copyright 2014 Hoegernet<br>
 * <br>
 *
 * @author Thorsten Hoeger
 *
 */
public class Starter {
	
	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			System.err.println("usage: restdoc2blueprint <url>");
		}
		String url = args[0];
		
		RestDoc doc = Starter.fetchDoc(url);
		
		Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();
		VelocityContext context = new VelocityContext();
		context.put("mathTool", new MathTool());
		context.put("dateTool", new DateTool());
		context.put("rd", doc);
		
		Template template = Velocity.getTemplate("velocity/blueprint.vm", CharEncoding.UTF_8);
		
		// try (OutputStreamWriter out = new OutputStreamWriter(System.out, CharEncoding.UTF_8);) {
		// template.merge(context, out);
		// } catch (IOException e) {
		// e.printStackTrace();
		// }
		try (FileWriter out = new FileWriter("blueprint.md");) {
			template.merge(context, out);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static RestDoc fetchDoc(String url) throws IOException {
		HttpResponse res = WS.url(url).options();
		if (WS.isStatusOK(res)) {
			String string = WS.getResponseAsString(res);
			return RestDocParser.parseString(string);
		}
		throw new IOException("invalid HTTP code: " + WS.getStatus(res));
	}
}
