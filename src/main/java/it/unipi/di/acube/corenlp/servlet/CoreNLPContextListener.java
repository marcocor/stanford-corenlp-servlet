package it.unipi.di.acube.corenlp.servlet;

import java.lang.invoke.MethodHandles;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.pipeline.StanfordCoreNLP;

public class CoreNLPContextListener implements ServletContextListener {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	public final static String CORENLP_PARAMS = "it.unipi.di.acube.corenlp.servlet.params";

	@Override
	public void contextInitialized(ServletContextEvent e) {
		LOG.info("Creating Stanford CoreNLP context.");
		ServletContext context = e.getServletContext();
		context.setAttribute("corenlp-pipeline", new StanfordCoreNLP());
	}

	@Override
	public void contextDestroyed(ServletContextEvent e) {
		LOG.info("Destroying Stanford CoreNLP context.");
	}
}
