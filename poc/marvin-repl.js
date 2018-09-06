// test for marvin repl
const { exec } = require('child_process');


var repl = require("repl");

function notebook() {
  exec('jupyter notebook', (err, stdout, stderr) => {
    if (err) {
      // node couldn't execute the command
    return;
  }
    // the *entire* stdout and stderr (buffered)
    console.log(`stdout: ${stdout}`);
    console.log(`stderr: ${stderr}`);
  });
}

var replServer = repl.start({
	prompt: "marvin >",
});

replServer.context.engine_generate = "Generating new engine !!!";
replServer.context.dryrun = "Vou rodar dryrun !!!!!";
replServer.context.notebook = notebook;
