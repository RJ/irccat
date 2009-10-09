<?php

/*
    Thanks to Jonty (http://jonty.co.uk/) for this script.
    We use it to see a feed of twitter search results for a given term in IRC.

    Needs php5.

    Run like:  php twitter-feed.php #twitters 'monkey' | nc -q0 localhost 12345
    (Where #twitters is the channel to pipe to, and monkey is the search term)

    If you want to filter things from the feed you can optionally add regexes 
    to the end of the params.
*/

    if (count($argv) < 3) {
        print "Usage: twitter-feed.php '#channel' 'searchterm' [url filter regexp, [url-filter regexp, ...]]\n";
        print "(Quotes are important, remember # is a comment in bash!)\n";
        exit;
    }

    array_shift($argv);
    $channel = array_shift($argv);
    $search = array_shift($argv);
    $filters = $argv ? $argv : array();

    print "{$channel} Starting twitter.com search feed for {$search}...\n";

    $originalUrl = "?since_id=4028479400&q=".urlencode($search);
    $baseUrl = "http://search.twitter.com/search";

    $firstRun = true;
    $refreshUrl = $originalUrl;

    while (1) {
        $source = file("{$baseUrl}{$refreshUrl}&refresh=true");
        if ($source) {
            $json = json_decode(implode('', $source));

            if ($json && isset($json->refresh_url)) {
                $refreshUrl = $json->refresh_url;

                if ($firstRun) {
                    $firstRun = false;
                    continue;
                }

                foreach ($json->results as $result) {
                    if ($message = processMessage($result->text, $filters)) {
                        print "{$result->from_user}: {$message}\n";
                    }
                }

            } else {
                // On error, reset to front page and retry
                $refreshUrl = $originalUrl;
            }
        }

        sleep(60);
    }

    function processMessage ($string, $filters) {
        $string = utf8_decode($string);

        // Resolve shortened URL's to the full thing for filtering
        preg_match_all('/http:\/\/[^ $)]+/i', $string, $urls);
        if ($urls) {
            foreach ($urls[0] as $url) {
                if ($redirect = getRedirect($url)) {
                    $string = str_replace($url, $redirect, $string);
                }
            }
        }

        $string = cleanString($string);
        if ($filters) {
            foreach ($filters as $filter) {
                if (preg_match("/{$filter}/i", $string)) {
                    $string = '';
                    break;
                }
            }
        }

        return $string;
    }

    function cleanString ($string) {
        $string = preg_replace('/\\\u([[:alnum:]]{4})/u', '?', $string);
        $string = str_replace('\/','/', urldecode($string));
        $string = html_entity_decode($string, null, 'UTF-8');
        return htmlspecialchars_decode($string);
    }

    function getRedirect ($url) {
        $curl = curl_init();
        curl_setopt($curl, CURLOPT_RETURNTRANSFER, true);
        curl_setopt($curl, CURLOPT_URL, $url);
        curl_setopt($curl, CURLOPT_HEADER, true);
        curl_setopt($curl, CURLOPT_NOBODY, true);
        $headers = curl_exec($curl);
        curl_close($curl);

        preg_match('/Location:(.*?)[\r\n]/i', $headers, $redirect);
        return $redirect ? urlencode(trim($redirect[1])) : '';
    }
