package it.unipi.di.acube.corenlp.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.stanford.nlp.io.IOUtils;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.LanguageInfo;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.PropertiesUtils;
import edu.stanford.nlp.util.StringUtils;

/**
 * Stanford CoreNLP Servlet offering an endpoint with the same interface as the standard CoreNLP server.
 * 
 * @author Marco Cornolti
 * @author Gabor Angeli (Original server code)
 * @author Arun Chaganty (Original server code)
 *
 */
public class CoreNlpPipelineServlet extends HttpServlet {
	private final static Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
	private static final long serialVersionUID = 1L;

	private static String defaultAnnotators = "tokenize,ssplit,pos,lemma,ner,parse,depparse,mention,coref,natlog,openie,regexner";

	private String defaultFormat = "json";

	private Properties getProperties(HttpServletRequest request) {
		// check if englishSR.ser.gz can be found (standard models jar doesn't have this)
		String defaultParserPath;
		ClassLoader classLoader = getClass().getClassLoader();
		URL srResource = classLoader.getResource("edu/stanford/nlp/models/srparser/englishSR.ser.gz");
		log("setting default constituency parser");
		if (srResource != null) {
			defaultParserPath = "edu/stanford/nlp/models/srparser/englishSR.ser.gz";
			log("using SR parser: edu/stanford/nlp/models/srparser/englishSR.ser.gz");
		} else {
			defaultParserPath = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
			log("warning: cannot find edu/stanford/nlp/models/srparser/englishSR.ser.gz");
			log("using: edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz instead");
			log("to use shift reduce parser download English models jar from:");
			log("http://stanfordnlp.github.io/CoreNLP/download.html");
		}

		// Default properties
		Properties props = PropertiesUtils.asProperties("annotators", defaultAnnotators, // Run these annotators by default
		        "mention.type", "dep", // Use dependency trees with coref by default
		        "coref.mode", "statistical", // Use the new coref
		        "coref.language", "en", // We're English by default
		        "inputFormat", "text", // By default, treat the POST data like text
		        "outputFormat", "json", // By default, return in JSON -- this is a server, after all.
		        "prettyPrint", "false", // Don't bother pretty-printing
		        "parse.model", defaultParserPath, // SR scales linearly with sentence length. Good for a server!
		        "parse.binaryTrees", "true", // needed for the Sentiment annotator
		        "openie.strip_entailments", "true"); // these are large to serialize, so ignore them

		// Add GET parameters as properties
		request.getParameterMap().entrySet().stream()
		        .filter(entry -> !"properties".equalsIgnoreCase(entry.getKey()) && !"props".equalsIgnoreCase(entry.getKey()))
		        .forEach(entry -> props.setProperty(entry.getKey(), request.getParameter(entry.getKey())));

		// Try to get more properties from query string.
		// (get the properties from the URL params)
		String urlPropStr = request.getParameter("properties");
		if (urlPropStr == null)
			urlPropStr = request.getParameter("props");
		if (urlPropStr != null)
			try {
				Map<String, String> propMap = StringUtils.decodeMap(URLDecoder.decode(urlPropStr, "UTF-8"));
				propMap.entrySet().stream().forEach(entry -> props.setProperty(entry.getKey(), propMap.get(entry.getKey())));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException("Could not decode properties.");
			}

		// check to see if a specific language was set, use language specific properties
		String language = (String) props.getOrDefault("pipelineLanguage", "default");

		if (language != null && !"default".equals(language)) {
			String languagePropertiesFile = LanguageInfo.getLanguagePropertiesFile(language);
			if (languagePropertiesFile != null) {
				try {
					Properties languageSpecificProperties = new Properties();
					languageSpecificProperties.load(IOUtils.getInputStreamFromURLOrClasspathOrFileSystem(languagePropertiesFile));
					PropertiesUtils.overWriteProperties(props, languageSpecificProperties);
				} catch (IOException e) {
					LOG.error("Failure to load language specific properties: " + languagePropertiesFile + " for " + language);
				}
			} else {
				throw new RuntimeException("Invalid language: '" + language);
			}
		}

		// (tweak the default properties a bit)
		if (!props.containsKey("mention.type")) {
			// Set coref head to use dependencies
			props.setProperty("mention.type", "dep");
			if (props.containsKey("annotators") && props.get("annotators") != null
			        && ArrayUtils.contains(((String) props.get("annotators")).split(","), "parse")) {
				// (case: the properties have a parse annotator --
				// we don't have to use the dependency mention finder)
				props.remove("mention.type");
			}
		}
		return props;
	}

	private StanfordCoreNLP getStanfordCoreNlp(Properties props) {
		if (getServletContext().getAttribute("cached-annotators") == null)
			getServletContext().setAttribute("cached-annotators", new HashMap<Properties, StanfordCoreNLP>());

		HashMap<Properties, StanfordCoreNLP> cache = (HashMap<Properties, StanfordCoreNLP>) getServletContext()
		        .getAttribute("cached-annotators");
		if (!cache.containsKey(props)) {
			LOG.info("Creating new annotator with properties: " + props);
			cache.put(props, new StanfordCoreNLP(props));
		}
		return cache.get(props);
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Properties props = getProperties(request);

		StanfordCoreNLP pipeline = getStanfordCoreNlp(props);

		String input = IOUtils.slurpInputStream(request.getInputStream(), request.getCharacterEncoding());

		LOG.info(input);

		Annotation annotation = new Annotation(input);
		pipeline.annotate(annotation);

		PrintWriter writer = response.getWriter();
		switch ((String) props.getOrDefault("outputFormat", defaultFormat)) {
		case "xml":
			response.setContentType("text/xml");
			pipeline.xmlPrint(annotation, writer);
			break;
		case "conll":
			response.setContentType("text/plain");
			pipeline.conllPrint(annotation, writer);
			break;
		default:
			response.setContentType("application/json");
			pipeline.jsonPrint(annotation, writer);
			break;
		}
		writer.flush();
	}

	public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
}
