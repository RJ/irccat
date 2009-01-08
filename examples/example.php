<?
/**
    Don't add stupid commands about ponies or paris hilton :)
**/
$input = $_SERVER['argv'][1];
$toks = explode(" ",$input);
$nick = array_shift($toks);
$channel = array_shift($toks);
$sender = array_shift($toks);
$first = array_shift($toks);

// switch on first word (the command word)
// print "nick: $nick  channel: $channel  sender: $sender  first: $first \n";

switch($first){
    
    case 'uptime':
        print `uptime`;
        break;

    case 'date':
        print `date`;
        print "Unix time: ".`date +%s`;
        break;

    case 'df':
        print `df -h | grep "^/dev"`;
        break;

    default:
        print "Don't know how to '$input'\n";        
        break;

}

exit;


?>
