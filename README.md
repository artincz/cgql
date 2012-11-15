Cgql
====

Oracle Coherence Groovy Query Language

Building
========

Coherence jar is not in public repos, so you have to download it separately and put it in `lib` folder.

Project is built by Gradle:

    $ ./gradlew build

Zip with Cgql distribution is created in build/distributions/cgql-1.0.zip.

To import project into Eclipse run:

    $ ./gradlew eclipse

and import the project from Eclipse.

Installation and Configuration
==============================

To take full advantage of Cgql console you must configure both Cgql console and all Coherence cluster nodes.
You can use Cgql console without configuring Coherence cluster nodes, but without Groovy filters and entry processors. Only Coherence built-in filters and entry processors can be used.

### Configuring Cgql console

1. Unzip the distribution package
1. Put jars with your domain classes to `userlib` dir (all jars in `userlib` will be on classpath)
1. Put your cache config xml to `userlib` dir (dir `userlib` is on classpath)
1. Put your POF config xml to `userlib` dir
1. Modify the `userlib/user-pof-config.xml` to include your POF config xml (using `<include>???.xml</include>`)
1. Export `JVM_OPTS` environment variable with Coherence options
  * Reference your cache config xml with `-Dtangosol.coherence.cacheconfig=...`
  * Reference `user-pof-config.xml` with `-Dtangosol.pof.config=user-pof-config.xml`
  * Include other Coherence options specific to your cluster
1. Run Cgql console with `bin/cgql.sh`

Example of JVM_OPTS definition:

    export JVM_OPTS="\                                                                                                                                                                               
      -Dtangosol.coherence.cluster=democluster \
      -Dtangosol.coherence.cacheconfig=user-cache-config.xml \
      -Dtangosol.pof.config=user-pof-config.xml"


### Configuring Coherence cluster nodes

1. Take the `lib/cgql-1.0.jar` and `lib/groovy-all-2.0.0.jar` from distribution package and put it on Coherence node classpath
1. include `cgql-pof-config.xml` in your main pof config (using `<include>cgql-pof-config.xml</include>`)


Examples
========

Classes used in examples are simple POJOs with getters/setters and toString().

    public class User {
      private String name;
      private int age;
      private List<Project> projects;
    }

    public class Project {
      private String name;
    }

After startup, there's a prompt which indicates, that no cache and no filter is chosen.

    (no cache) (no filter) >

So we select a cache for this example.

    (no cache) (no filter) > cache 'DemoCache'

Let's fill some data with `put key, value` command.

    DemoCache (no filter) > put 1, new cz.artin.cgql.demo.User(name: 'Kenny', age: 8, projects: [new cz.artin.cgql.demo.Project(name: 'project kenny-1'), new cz.artin.cgql.demo.Project(name: 'project kenny-2')])
    DemoCache (no filter) > put 2, new cz.artin.cgql.demo.User(name: 'Kyle', age: 9, projects: [new cz.artin.cgql.demo.Project(name: 'project kyle-1')])
    DemoCache (no filter) > put 3, new cz.artin.cgql.demo.User(name: 'Stan', age: 10, projects: [new cz.artin.cgql.demo.Project(name: 'project stan-1')])
    DemoCache (no filter) > put 4, new cz.artin.cgql.demo.User(name: 'Cartman', age: 11, projects: [new cz.artin.cgql.demo.Project(name: 'project cart-1')])

With `count()` command we check there are 4 entries.

    DemoCache (no filter) > count()
    4

We can list just keys with `keys()` command.

    DemoCache (no filter) > keys()
    1
    4
    2
    3
    4 items

Or just values with `values()` command.

    DemoCache (no filter) > values()
    User [name=Kyle, age=9]
    User [name=Kenny, age=8]
    User [name=Cartman, age=11]
    User [name=Stan, age=10]
    4 items

Or whole entries with `entries()` command.

    DemoCache (no filter) > entries()
    key=1, value=User [name=Kenny, age=8]
    key=2, value=User [name=Kyle, age=9]
    key=3, value=User [name=Stan, age=10]
    key=4, value=User [name=Cartman, age=11]
    4 items

We can filter results of all commands several ways.
By single key:

    DemoCache (no filter) > key 2
    DemoCache key=2 > entries()
    key=2, value=User [name=Kyle, age=9]
    1 items

By multiple keys:

    DemoCache key=2 > keys 2,3
    DemoCache keys=[2, 3] > entries()
    key=2, value=User [name=Kyle, age=9]
    key=3, value=User [name=Stan, age=10]
    2 items

Custom groovy filter. String in quotes can be arbitrary Groovy script. Type of `entry` variable is java.util.Map.Entry, for some cache types it can be some subclass, eg. for partitioned cache with POF serializer it's BinaryEntry, so you can access the entry in binary and deserialize only attributes you need.

    DemoCache keys=[2, 3] > filter 'entry.key<=2'
    DemoCache filter='entry.key<=2' > entries()
    key=1, value=User [name=Kenny, age=8]
    key=2, value=User [name=Kyle, age=9]
    2 items

The groovy script can contain really anything, so you can filter by properties of nested collections, which is not possible in CohQL.

    DemoCache filter='entry.key<=2' > filter 'entry.value.projects.size()==2'
    DemoCache filter='entry.value.projects.size()==2' > entries()
    key=1, value=User [name=Kenny, age=8]
    1 items

You can also use Coherence built-in extractors.

    DemoCache filter='entry.value.projects.size()==2' > filter new EqualsFilter(new PofExtractor(null, 1), 'Stan')
    DemoCache filter=EqualsFilter(PofExtractor(target=VALUE, navigat... > entries()
    key=3, value=User [name=Stan, age=10]
    1 items

For safety reasons write operations require explicit filter, if you want to target all entries, use built-in AlwaysFilter.
EntryProcessors are potentionaly write operations, so we specify AlwaysFilter.

    DemoCache filter=EqualsFilter(PofExtractor(target=VALUE, navigat... > filter new AlwaysFilter()

String parameter makes body of EntryProcessor, result of EntryProcessor is result in Groovy script (last statement in this case).

    DemoCache filter=AlwaysFilter > process 'entry.value.name'
    key=2, value=Kyle
    key=1, value=Kenny
    key=4, value=Cartman
    key=3, value=Stan
    4 items

Some more advanced EntryProcessor body.

    DemoCache filter=AlwaysFilter > process 'if (entry.value.age<10) return entry.value.name+" is young" else return entry.value.name+" is old"'
    key=2, value=Kyle is young
    key=1, value=Kenny is young
    key=4, value=Cartman is old
    key=3, value=Stan is old
    4 items

If you want to make some changes in entry, you must 'commit' them with the 'magic' statement `entry.value=entry.value`.

    DemoCache filter=AlwaysFilter > process 'entry.value.age++; entry.value=entry.value; return entry.value.age'
    key=2, value=10
    key=1, value=9
    key=4, value=12
    key=3, value=11
    4 items

Changed values.

    DemoCache filter=AlwaysFilter > entries()
    key=1, value=User [name=Kenny, age=9]
    key=2, value=User [name=Kyle, age=10]
    key=3, value=User [name=Stan, age=11]
    key=4, value=User [name=Cartman, age=12]
    4 items

Cohq console is Groovy console.

    DemoCache filter=AlwaysFilter > 3.times{println 'Hello'}
    Hello
    Hello
    Hello

Result of commands `keys()` and `values()` is java.util.Collection, `entries()` and `process()` returns java.util.Map.
You can work with these results in console.

    DemoCache filter=AlwaysFilter > keys().size()
    4
    
    DemoCache filter=AlwaysFilter > keys().collect{-it}
    -2
    -1
    -4
    -3
    4 items

This command fetches whole entries over the wire and `age` property is summed in Cqgl console.

    DemoCache filter=AlwaysFilter > values().sum{it.age}
    42

Here the `age` property is extracted with EntryProcessor and only neccessary data are sent over the wire.

    DemoCache filter=AlwaysFilter > process('entry.value.age').values().sum()
    42

You can write more statements on one line separated with semicolon.
Results can be assigned to variables.

    DemoCache filter=AlwaysFilter > key 1; kenny=values()[0]
    User [name=Kenny, age=9]

... and used later

    DemoCache key=1 > kenny.name.collect{it + ' '}.join()
    K e n n y 

Entries can be deleted with `delete()` command.

    DemoCache key=1 > delete()

There are 3 entries left.

    DemoCache key=1 > filter null
    DemoCache (no filter) > count()
    3
