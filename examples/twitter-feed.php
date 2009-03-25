<?php
/*
    Thanks to Jonty (http://i.am.jonty.co.uk/) for this script.
    We use it to see a feed of twitter search results for a given term in IRC.

    Needs php5.

    Run like:  php twitter-feeder.php | nc -q0 localhost 12345
*/

    $originalUrl = "?since_id=0&q=last.fm"; // <-- term in there.
    $mainUrl = "http://search.twitter.com/search";

    print "#playdar Starting search.twitter.com feed...\n";

    $firstRun = 1;
    $nextUrl = $originalUrl;
    while(1){
        $pageContent = file($mainUrl.$nextUrl.'&refresh=true');
        if ($pageContent) {
            $source = implode("", $pageContent);
            if (preg_match("/\"refresh_url\":\"(.*?)\"/", $source, $urlMatches)) {
                $nextUrl = $urlMatches[1];

                preg_match_all("/{\"text\":\"(.*?)\",\"to_user_id\":.*?,\"from_user\":\"(.*?)\".*?}/sm", $source, $postMatches, PREG_SET_ORDER);
                foreach($postMatches as $match){
                    $message = cleanString($match[1]);
                    if($firstRun == 0) print "{$match[2]}: ".$message."\n";
                }
                if($firstRun == 1) $firstRun = 0;
            } else {
                // On error, reset to front page and retry
                $nextUrl = $originalUrl;
            }
        }

        sleep(60);
    }

    function cleanString($string) {
        $string = preg_replace('/\\\u([[:alnum:]]{4})/u', '?', $string);
        $string = str_replace('\/','/', urldecode($string));
        $string = html_entity_decode($string, null, 'UTF-8');
        $string = htmlspecialchars_decode($string);
        $string = utf8_decode($string);
        return $string;
    }

?>
