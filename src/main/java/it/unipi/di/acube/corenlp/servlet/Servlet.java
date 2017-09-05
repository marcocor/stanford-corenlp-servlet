package it.unipi.di.acube.corenlp.servlet;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.invoke.MethodHandles;

import javax.servlet.ServletContext;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * @author Marco Cornolti
 */

@Path("/")
public class Servlet {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Context
	ServletContext context;

	@POST
	@Path("/json")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({ MediaType.APPLICATION_JSON })
	public Response getJson(String text) {
		LOG.info("Annotating " + preview(text));
		StanfordCoreNLP pipeline = (StanfordCoreNLP) context.getAttribute("corenlp-pipeline");
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);

		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream os) throws IOException, WebApplicationException {
				Writer writer = new BufferedWriter(new OutputStreamWriter(os));
				pipeline.jsonPrint(annotation, writer);
				writer.flush();
			}
		};

		return Response.ok(stream).build();
	}

	@POST
	@Path("/xml")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({ MediaType.APPLICATION_XML })
	public Response getXml(String text) {
		LOG.info("Annotating " + preview(text));
		StanfordCoreNLP pipeline = (StanfordCoreNLP) context.getAttribute("corenlp-pipeline");
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);

		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream os) throws IOException, WebApplicationException {
				Writer writer = new BufferedWriter(new OutputStreamWriter(os));
				pipeline.xmlPrint(annotation, writer);
				writer.flush();
			}
		};

		return Response.ok(stream).build();
	}

	@POST
	@Path("/conll")
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces({ MediaType.TEXT_PLAIN })
	public Response getConll(String text) {
		LOG.info("Annotating " + preview(text));
		StanfordCoreNLP pipeline = (StanfordCoreNLP) context.getAttribute("corenlp-pipeline");
		Annotation annotation = new Annotation(text);
		pipeline.annotate(annotation);

		StreamingOutput stream = new StreamingOutput() {
			public void write(OutputStream os) throws IOException, WebApplicationException {
				Writer writer = new BufferedWriter(new OutputStreamWriter(os));
				pipeline.conllPrint(annotation, writer);
				writer.flush();
			}
		};

		return Response.ok(stream).build();
	}

	public static String preview(String body) {
		body = body.substring(0, Math.min(100, body.length())).replaceAll("\\s", " ");
		return String.format("%S (%d chars)", body, body.length());
	}
}
