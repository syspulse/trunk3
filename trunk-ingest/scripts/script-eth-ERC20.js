var hash = inputs.hash();
var logs = inputs.logs();
var transfers = [];
// print(">>",logs,logs.length);
var n = 0;
for (var i=0; i<logs.length; ++i) {    
    // print(">",logs[i]);
    var data = logs[i].data();
    var topics = logs[i].topics();
    var topic0 = topics[0];
    if(topic0 == "0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef") {
        var from = topics[1];
        var to = topics[2];
        var value = Number(data)
        transfers[n] = from + " -> " + to + ": "+value;
        n++;
    }
}

// print(">>>",transfers);
if(transfers.length == 0) {
    null;
} else {
    var output = transfers;
    var res = {tx_hash: hash, output: output};
    Java.asJSONCompatible(res);
}

