package com.test;

import javax.swing.text.*;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
//import javax.swing.text.html.parser.Element;
import java.io.*;
import java.net.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Sample implementation of google search result crawler.
 */
public class WebCrawler {

    private static final String GOOGLE_SEARCH_URL = "https://www.google.com/search?q=";
    private static final String SCRIPT_TAG_NAME = "script";
    private static final String FILE_EXTENSION = ".js";
    private static final String USER_AGENT = "Mozilla/5.0";


    public static void main(String[] args) throws IOException, BadLocationException, URISyntaxException {
        String term = readSearchTerm();
        System.out.println("Searching result for term = " + term);

        String googleResultPage = downloadHtmlPage(GOOGLE_SEARCH_URL + term);

        List<String> searchResultList = googleResultParser(googleResultPage);
        ConcurrentMap<String, Integer> libOccurencesMap = new ConcurrentHashMap<>();


        for (String result : searchResultList) {
            countJsLibsOnPage(result, libOccurencesMap);
        }

        libOccurencesMap.entrySet()
                .stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .forEach(entry -> System.out.println("fileName = " + entry.getKey() + " count = " + entry.getValue()) );
    }

    private static String readSearchTerm() throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        System.out.println("Enter search term ...");
        return br.readLine();
    }

    private static void countJsLibsOnPage(String result, ConcurrentMap<String, Integer> libOccurencesMap) throws IOException, BadLocationException, URISyntaxException {

        String htmlPage = downloadHtmlPage(result);
        ElementIterator iterator = getElementIterator(htmlPage);
        Element element;

        while ((element = iterator.next()) != null) {
            if (element.getName().equalsIgnoreCase(SCRIPT_TAG_NAME)) {
                String srcUrl = (String) element.getAttributes().getAttribute(HTML.Attribute.SRC);
                if (srcUrl != null && srcUrl.contains(FILE_EXTENSION)) {
                    String filename = Paths.get(new URI(srcUrl).getPath()).getFileName().toString();

                    if(libOccurencesMap.containsKey(filename)){
                        int counter = libOccurencesMap.get(filename);
                        libOccurencesMap.put(filename, ++counter);
                    }else {
                        libOccurencesMap.put(filename, 1);
                    }
                }
            }
        }

    }

    private static ElementIterator getElementIterator(String htmlPage) throws IOException, BadLocationException {

        HTMLEditorKit htmlKit = new HTMLEditorKit();
        HTMLDocument htmlDoc = (HTMLDocument) htmlKit.createDefaultDocument();
        htmlDoc.putProperty("IgnoreCharsetDirective", Boolean.TRUE);
        htmlKit.read(new StringReader(htmlPage), htmlDoc, 0);

        return new ElementIterator(htmlDoc);
    }

    /**
     * Extract site urls from google result page
     * @param htmlPage
     * @return list of parsed Urls
     * @throws IOException
     * @throws BadLocationException
     */
    private static List<String> googleResultParser(String htmlPage) throws IOException, BadLocationException {
        List<String> result = new ArrayList<>();
        ElementIterator iterator = getElementIterator(htmlPage);
        Element element;

        while ((element = iterator.next()) != null) {
            AttributeSet as = element.getAttributes();
            Object name = as.getAttribute(StyleConstants.NameAttribute);
            if (name == HTML.Tag.H3) {
                String cssClass = (String) as.getAttribute(HTML.Attribute.CLASS);

                if (cssClass.equalsIgnoreCase("r")) {
                    Element urlElement = element.getElement(0);
                    SimpleAttributeSet hrefAttributes = (SimpleAttributeSet) urlElement.getAttributes().getAttribute(HTML.Tag.A);
                    String url = (String) hrefAttributes.getAttribute(HTML.Attribute.HREF);
                    int startIndexOfUrl = url.indexOf("http");
                    int endIndexOfUrl = url.indexOf("&sa=");
                    if (startIndexOfUrl >= 0) {
                        result.add(url.substring(startIndexOfUrl, endIndexOfUrl));
                    }
                }
            }
        }

        return result;
    }


    /**
     * Download html page for given URL
     * @param pageUrl
     * @return
     */
    private static String downloadHtmlPage(String pageUrl) {

        InputStream is = null;
        BufferedReader br;
        String line;
        StringBuffer stringBuffer = new StringBuffer();

        try {
            URLConnection urlConnection = new URL(pageUrl).openConnection();
            urlConnection.addRequestProperty("User-Agent", USER_AGENT);

            is = urlConnection.getInputStream();  // throws an IOException
            br = new BufferedReader(new InputStreamReader(is));

            while ((line = br.readLine()) != null) {
                stringBuffer.append(line);
            }
        } catch (MalformedURLException mue) {
            mue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        } finally {
            try {
                if (is != null) is.close();
            } catch (IOException ioe) {
            }
        }

        return stringBuffer.toString();
    }

}
