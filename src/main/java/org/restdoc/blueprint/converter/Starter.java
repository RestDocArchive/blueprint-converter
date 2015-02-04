package org.restdoc.blueprint.converter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.CharEncoding;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.MathTool;
import org.restdoc.api.ParamDefinition;
import org.restdoc.api.ParamValidation;
import org.restdoc.api.RestDoc;
import org.restdoc.api.RestResource;
import org.restdoc.api.util.RestDocParser;

/**
 * Copyright 2014 Hoegernet<br>
 * <br>
 *
 * @author Thorsten Hoeger
 *
 */
public class Starter {
	
	@SuppressWarnings("resource")
	public static void main(String[] args) throws IOException {
		if (args.length != 2) {
			System.out.println("Usage: blueprint-converter <input> <output>");
			System.exit(1);
		}
		final InputStream is;
		if (args[0].equals("-")) {
			is = System.in;
		} else {
			is = new FileInputStream(new File(args[0]));
		}
		
		String restdoc;
		try (BufferedReader read = new BufferedReader(new InputStreamReader(is))) {
			StringBuilder sb = new StringBuilder();
			while (read.ready()) {
				sb.append(read.readLine());
			}
			restdoc = sb.toString();
		}
		
		RestDoc doc = RestDocParser.parseString(restdoc);
		Starter.cleanPaths(doc);
		
		Velocity.setProperty(RuntimeConstants.RESOURCE_LOADER, "class");
		Velocity.setProperty("class.resource.loader.class", "org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader");
		Velocity.init();
		VelocityContext context = new VelocityContext();
		context.put("mathTool", new MathTool());
		context.put("dateTool", new DateTool());
		context.put("rd", doc);
		
		Template template = Velocity.getTemplate("velocity/blueprint.vm", CharEncoding.UTF_8);
		
		final Writer wr;
		if (args[1].equals("-")) {
			wr = new OutputStreamWriter(System.out, CharEncoding.UTF_8);
		} else {
			wr = new FileWriter(args[1]);
		}
		
		template.merge(context, wr);
		wr.close();
	}
	
	private static void cleanPaths(RestDoc doc) {
		for (RestResource rr : doc.getResources()) {
			String path = rr.getPath();
			Pattern pattern = Pattern.compile("\\{.*?\\}");
			Matcher matcher = pattern.matcher(path);
			while (matcher.find()) {
				String segment = matcher.group();
				if (segment.indexOf(':') == -1) {
					continue;
				}
				String param = segment.substring(1, segment.indexOf(':'));
				path = path.replace(segment, "{" + param + "}");
				ParamDefinition paramDefinition = rr.getParams().get(param);
				if (paramDefinition != null) {
					ParamValidation val = new ParamValidation("matches", segment.substring(segment.indexOf(':') + 1, segment.length() - 1));
					paramDefinition.getValidations().add(val);
				}
			}
			rr.setPath(path);
		}
	}
}
