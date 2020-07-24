# sharepoint-datasource-regex-utility
Simplifies the creation of datasources for SharePoint optimized connectors.

Run with: `java -jar ./build/libs/sharepoint-datasource-regex-utility-1.0.jar {args}`

```
 -app VAL            : The Fusion application that contains the datasource you
                       want to update.
 -datasourceId VAL   : Datasource ID you want to update.
 -fusionPassword VAL : Fusion password. If you don't specify this here, you
                       will be prompted for it.
 -fusionUrl VAL      : Fusion url. E.g. http://localhost:8764
 -fusionUsername VAL : Fusion username.
 -link VAL           : Links to specific resources you want to crawl. Sites,
                       document libraries / lists.
 -linkFile VAL       : File containing links to specific resources you want to
                       crawl. Sites, document libraries / lists.
 -proxyHost VAL      : Proxy host you need to use to get to Fusion if
                       applicable.
 -proxyPort N        : Proxy port you need to use to get to Fusion if
                       applicable. (default: 0)
 -proxyScheme VAL    : Proxy scheme - http or https.
```

The SharePoint optimized datasource configuration 'start links' only allow site collection urls to be specified. But often times you want to crawl just certain sites, document libraries, lists, list items, etc. So this utility provides you a way to get that level of granularity in the new tool. You specify each item you actually want to crawl. And this will update your SharePoint datsource with inclusive regexes that will do so.
