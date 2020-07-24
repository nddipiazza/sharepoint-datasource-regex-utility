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

The SharePoint optimized datasource configuration 'start links' only allow site collection urls to be specified.

But often times you want to crawl just certain sites, document libraries, lists, list items, etc. The deprecated sharepoint connector supported this. 

The optimized connector does this level of granularity using Regexes. So this utility provides you a way to feed it a list of URLs you want to index, and it will spit out the `includeRegexes` list that will get your datasource to crawl that way. 

You specify each item you actually want to crawl. And this will update your SharePoint datsource with inclusive regexes that will do so.

Example:

`java -jar ./build/libs/sharepoint-datasource-regex-utility-1.0.jar -link "https://some.sharepoint.host/Lists/noindexlist" -link "https://some.sharepoint.host/sites/mysitecol/site1/site2/site3/MyList here" -link "https://tenant.sharepoint.com/files_with_special_chars_in_filesnames/Shared%20Documents" -fusionUrl http://yourfusionhost:8764 -fusionUsername YOURUSERNAME -fusionPassword YOURPASSWORD -app YOURTEST -datasourceId YOURDSID`


Results in:

```
https://some\.sharepoint\.host
https://some\.sharepoint\.host/Lists
https://some\.sharepoint\.host/Lists/noindexlist
https://some\.sharepoint\.host/Lists/noindexlist/.*
https://some\.sharepoint\.host/sites
https://some\.sharepoint\.host/sites/mysitecol
https://some\.sharepoint\.host/sites/mysitecol/site1
https://some\.sharepoint\.host/sites/mysitecol/site1/site2
https://some\.sharepoint\.host/sites/mysitecol/site1/site2/site3
https://some\.sharepoint\.host/sites/mysitecol/site1/site2/site3/MyList here
https://some\.sharepoint\.host/sites/mysitecol/site1/site2/site3/MyList here/.*
https://tenant\.sharepoint\.com
https://tenant\.sharepoint\.com/files_with_special_chars_in_filesnames
https://tenant\.sharepoint\.com/files_with_special_chars_in_filesnames/Shared Documents
https://tenant\.sharepoint\.com/files_with_special_chars_in_filesnames/Shared Documents/.*
https://tenant\.sharepoint\.com/files_with_special_chars_in_filesnames/Shared%20Documents
https://tenant\.sharepoint\.com/files_with_special_chars_in_filesnames/Shared%20Documents/.*
```

And after this runs, you will notice the inclusive regexes are added to your datasource config.

**Very important**: In order to crawl ACL permissions in incremental crawls, the parent of each item must be present in the index. So for example, you cannot crawl a list item but exclude the list it came from. This is because inherited permissions rely on the parent items to be present in the index when getting updates to a child item.

So if you do not want to see the Parent item in search results, you must exclude these parent items using the query pipeline. Do not exclude it from the Index pipeline. 
