import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.io.File;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("unused")
public class DatasourceConfigGenerator {

    @Option(name = "-link", usage = "Links to specific resources you want to crawl. Sites, document libraries / lists.")
    private List<String> links;

    @Option(name = "-linkFile", usage = "File containing links to specific resources you want to crawl. Sites, document libraries / lists.")
    private String file;

    @Option(name = "-fusionUrl", usage = "Fusion url. E.g. http://localhost:8764")
    private String fusionUrl;

    @Option(name = "-fusionUsername", usage = "Fusion username.")
    private String fusionUsername;

    @Option(name = "-fusionPassword", usage = "Fusion password. If you don't specify this here, you will be prompted for it.")
    private String fusionPassword;

    @Option(name = "-app", usage = "The Fusion application that contains the datasource you want to update.")
    private String app;

    @Option(name = "-datasourceId", usage = "Datasource ID you want to update.")
    private String dsId;

    @Option(name = "-proxyHost", usage = "Proxy host you need to use to get to Fusion if applicable.")
    private String proxyHost;

    @Option(name = "-proxyPort", usage = "Proxy port you need to use to get to Fusion if applicable.")
    private int proxyPort;

    @Option(name = "-proxyScheme", usage = "Proxy scheme - http or https.")
    private String proxyScheme;

    public static void main(String [] args) throws Exception {
        DatasourceConfigGenerator configGen = new DatasourceConfigGenerator();
        CmdLineParser parser = new CmdLineParser(configGen);
        try {
            parser.parseArgument(args);
            configGen.run();
        } catch (CmdLineException e) {
            parser.printUsage(System.out);
            System.out.println("The SharePoint optimized datasource configuration 'start links' only allow site collection urls to be specified. " +
                "But often times you want to crawl just certain sites, document libraries, lists, list items, etc. " +
                "So this utility provides you a way to get that level of granularity in the new tool. You specify each item you actually want to crawl. And this will " +
                "update your SharePoint datsource with inclusive regexes that will do so.");
            System.out.println(e.getLocalizedMessage());
            throw e;
        }
    }

    private void run() throws Exception {
        if (StringUtils.isBlank(file) && links == null) {
            throw new CmdLineException("Either -f or one or more -l must be specified");
        }
        if (StringUtils.isNotBlank(file)) {
            links = FileUtils.readLines(new File(file), StandardCharsets.UTF_8);
        }
        Set<String> regexes = new TreeSet<>();
        for (String link : links) {
            URL url = new URL(link);
            String portStr = url.getPort() == -1 ? "" : (":" + url.getPort());
            String path = url.getPath();
            for (int idx = path.indexOf('/'); idx != -1; idx = path.indexOf('/', idx + 1)) {
                regexes.add(escapeLink(url.getProtocol() + "://" + url.getHost() + portStr + path.substring(0, idx)));
            }
            String decodedPath = URLDecoder.decode(path, StandardCharsets.UTF_8.name());
            String newLink = url.getProtocol() + "://" + url.getHost() + portStr + path;
            regexes.add(escapeLink(newLink));
            regexes.add(escapeLink(newLink) + "/.*");
            String decLink = url.getProtocol() + "://" + url.getHost() + portStr + decodedPath;
            regexes.add(escapeLink(decLink));
            regexes.add(escapeLink(decLink) + "/.*");
        }

        regexes.forEach(System.out::println);

        if (StringUtils.isBlank(fusionUrl)) {
            return;
        }

        ObjectMapper objectMapper = new ObjectMapper();
        HttpClientBuilder builder = HttpClientBuilder.create();
        if (StringUtils.isNotBlank(proxyHost)) {
            builder.setProxy(new HttpHost(proxyHost, proxyPort, proxyScheme));
        }
        try (CloseableHttpClient client = builder.build()) {
            HttpGet dsGet = new HttpGet(fusionUrl + "/api/apollo/apps/" + app + "/connectors/datasources/" + dsId);
            String auth = fusionUsername + ":" + fusionPassword;
            byte[] encodedAuth = Base64.encodeBase64(
                auth.getBytes(StandardCharsets.ISO_8859_1));
            String authHeader = "Basic " + new String(encodedAuth);
            dsGet.setHeader(HttpHeaders.AUTHORIZATION, authHeader);
            CloseableHttpResponse response = client.execute(dsGet);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Status from fusion getting datasource: " + response.getStatusLine().getStatusCode() + " , " + IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            }
            Map ds = objectMapper.readValue(response.getEntity().getContent(), Map.class);
            Map properties = (Map)ds.get("properties");

            List<String> includeRegexes = new ArrayList<>(regexes);
            properties.put("includeRegexes", includeRegexes);

            // set the new regexes here.

            HttpPut dsPut = new HttpPut(fusionUrl + "/api/apollo/apps/" + app + "/connectors/datasources/" + dsId);
            dsPut.setHeader("Content-Type", "application/json");
            dsPut.setEntity(new StringEntity(objectMapper.writeValueAsString(ds)));
            response = client.execute(dsPut);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new Exception("Status from fusion saving datasource: " + response.getStatusLine().getStatusCode() + " , " + IOUtils.toString(response.getEntity().getContent(), StandardCharsets.UTF_8));
            }
        }

    }

    private String escapeLink(String link) {
        return link.replaceAll("([\\\\+*?\\[\\](){}|.^$])", "\\\\$1");
    }
}
