package cz.artin.cgql.client

import jline.console.ConsoleReader

import org.apache.commons.lang.ObjectUtils
import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.control.CompilationFailedException
import org.codehaus.groovy.control.CompilerConfiguration

import com.tangosol.net.CacheFactory
import com.tangosol.net.NamedCache
import com.tangosol.util.Filter

import cz.artin.cgql.server.GroovyFilter

/**
 * @author Pavel Cernocky
 *
 */
public class CgqlApp {

  public static void main(String[] args) {
    def cli = new CliBuilder(usage: 'cgql')
    cli.width = 200
    cli.with {
      h longOpt: 'help', 'Prints help'
      e longOpt: 'execute', args:1, argName: 'command', 'Executes expression and exits'
      f longOpt: 'file', args:1, argName: 'file', 'Executes all commands from file and exits, if file is \'-\' commands are read from stdin'
    }

    def options = cli.parse(args)
    if (!options || options.h || !options.arguments().empty) {
      cli.usage()
      return
    }

    def commandsReader
    if (options.e) {
      commandsReader = new StringReader(options.e)
    }
    else if (options.f) {
      if (options.f == '-') {
        commandsReader = System.in
      }
      else {
        commandsReader = new File(options.f).newReader('UTF-8')
      }
    }

    if (commandsReader != null) {
      new CgqlApp().run(commandsReader)
    }
    else {
      new CgqlApp().runInteractive()
    }
  }

  GroovyShell shell
  Binding shellContext = new Binding()

  void run(Reader commands) {
    startup()
    try {
      for (;;) {
        String command = commands.readLine()
        if (command == null) {
          break
        }

        execute(command)
      }
    }
    finally {
      shutdown()
    }
  }

  void runInteractive() {
    startup()
    try {
      ConsoleReader reader = new ConsoleReader()
      reader.setExpandEvents(false)
      reader.setPrompt(getPrompt())
      reader.setEchoCharacter(new Character((char)0))

      for (;;) {
        String command = reader.readLine(getPrompt())
        if (command == null) {
          break
        }

        executeInteractive(command)
      }
    }
    finally {
      shutdown()
    }
  }

  void startup() {
    shellContext.cache = null
    shellContext.filter = null

    def compilerConfig = new CompilerConfiguration()
    compilerConfig.scriptBaseClass = CgqlScript.class.name

    shell = new GroovyShell(shellContext, compilerConfig)
  }

  void shutdown() {
    CacheFactory.shutdown()
  }

  static final int PROMPT_FILTER_MAX_LENGTH = 50

  String getPrompt() {
    String prompt

    // cache
    if (shellContext.cache == null) {
      prompt = '(no cache) '
    }
    else {
      prompt = shellContext.cache + ' '
    }

    // filter
    if (shellContext.filter == null) {
      prompt += "(no filter) "
    }
    else if (shellContext.filter instanceof GroovyFilter) {
      prompt += "filter='${StringUtils.abbreviate(shellContext.filter.expression, PROMPT_FILTER_MAX_LENGTH)}' "
    }
    else if (shellContext.filter instanceof Filter) {
      prompt += "filter=${StringUtils.abbreviate(shellContext.filter.toString(), PROMPT_FILTER_MAX_LENGTH)} "
    }
    else if (shellContext.filter instanceof Collection) {
      String keysStr = ''
      for (Iterator<Object> iter = shellContext.filter.iterator(); iter.hasNext(); ) {
        if (keysStr.length() > PROMPT_FILTER_MAX_LENGTH) {
          keysStr += '...'
          break
        }
        keysStr += iter.next().toString() + (iter.hasNext() ? ', ' : '')
      }
      prompt += "keys=[$keysStr] "
    }
    else {
      prompt += "key=${shellContext.filter} "
    }

    return prompt + '> '
  }

  void executeInteractive(String command) {
    long start = System.currentTimeMillis()
    try {
      Object res = execute(command)
      printRes(res)
      println "Processing time: ${System.currentTimeMillis() - start} ms"
    }
    catch (CompilationFailedException e) {
      println e.message
    }
    catch (MessageException e) {
      println e.message
    }
    catch (Exception e) {
      e.printStackTrace()
    }
  }

  void printRes(Object res) {
    if (res instanceof NamedCache) {
      // dont print anything, otherwise whole cache content would be printed (NamedCache is instance of Map)
    }
    else if (res instanceof Iterator) {
      printIteratorRes(res)
    }
    else if (res instanceof Iterable) {
      printIteratorRes(res.iterator())
    }
    else if (res instanceof Map) {
      printMapRes(res)
    }
    else if (res instanceof Map.Entry) {
      printEntryRes(res)
    }
    else {
      println toString(res)
    }
  }

  void printIteratorRes(Iterator iter) {
    int cnt = 0
    while (iter.hasNext()) {
      println toString(iter.next())
      cnt++
    }
    println "$cnt items"
  }

  void printMapRes(Map map) {
    int cnt = 0
    for (Map.Entry entry : map.entrySet()) {
      printEntryRes(entry)
      cnt++
    }
    println "$cnt items"
  }

  void printEntryRes(Map.Entry entry) {
    println 'key=' + toString(entry.getKey()) + ', value=' + toString(entry.getValue())
  }

  String toString(Object obj) {
    return ObjectUtils.toString(obj, "<null>")
  }

  Object execute(String command) {
    return shell.evaluate(CgqlScript.IMPORTS + command)
  }

}
