Hercules
========

*NOTE:* This this project is under heavy development and anything can change without previous notice (and a lot of the functionality is not in place yet).

*Quick start:*
This was way to much text! How do I install Hercules (on a Redhat based system)?

    # Make sure you have Java 7 and sbt installed
    git clone https://github.com/Molmed/hercules.git &&\
    sbt rpm:packageBin &&\
    sudo yum install target/rpm/RPMS/noarch/hercules-*.noarch.rpm
    # Read the section on configuration to get a proper setup of your roles!
    
Or try it out on the Vagrant machines defined in this repo (you'll need to have Vagrant and VirtualBox installed):

    vagrant up

At the moment you'll have to installed Hercules your self on the nodes - but we are working on setting it all up with Ansible. Check out our public ansible role [here](https://www.github.com/Molmed/roles_hercules).

*Background:*

The amazing Hercules project, taking the stone out of Sisyphus hands! Hercules is a project at the [SNP&SEQ Technology Platform](http://www.sequencing.se) which aims at automating the data flow at our sequencing facilty. Our workflow looks as follows:

 * Convert raw data (bcl files) and demultiplex it at local systems
 * Transfer the data to a remote location (in this case UPPMAX) where processing and delivery is carried out
 * Split the data into one or different projects
 * Calculate per project quality metrics
 * Deliver data to the end user
 * Deposit data for long term storage (at SweStore)
 * Remove data from local storage

We have previously run these steps semi-manually with the help of [Sisyphus](https://www.github.com/Molmed/sisyphus). The first goal of Hercules is to provide a way to take away the hands-on parts of the Sisyphus workflow. Thus Sisyphus is used as a backend for a lot of the tasks carried our by Hercules. Hercules, is however, not restricted to using Sisyphus but should be able to support other backends as necessary.

Hercules is built as a actor-based distributed system using the [Akka framework](http://akka.io). We use the Akka cluster functionality to be able to start Hercules nodes on different machines and assign them with one or more roles. Futhermore Hercules exposes a REST API which can be used to control it either through a commandline client or a web based front-end (which are both upcoming).

*Key concepts and features*

To understand how Hercules works it's important to grasp a few key concepts.

**The cluster:** The Hercules system consists of one or more nodes (running Hercules instances). These constitute a cluster.
**Nodes:** Each running Hercules instances is called a node.
**Roles:** Each node can run one or more roles. Examples of roles are: Master and demultiplexer. Each roles contains one part of the workflow functionallity. The roles communicate with each other through sending messages, and this is independent of if the roles are running on the same node or not.
**Master:** The actor which acts as the centralizing point in the workflow. Messages from other roles will pass through the master, which will decide what to do with them. 

Defining nodes and roles makes Hercules very flexible in how it is able to run. It's possible to run Hercules with all required roles on a single node, or to split the roles out over serveral nodes. The later is useful as it allows you run some long runing and resoruce intensive operations (such as the demultiplexing) on separate nodes.

It also makes Hercules highly resilient to errors. An error in one part of the system should not propagate and disturb other parts of the system. Nodes can be taken down and up again, and they will then re-attach themselves to the cluster. Futhermore the mater actor has a persistance mechanism which makes sure that data will not be lost.

Configuration
-------------

As previously noted we maintain a [Ansible role](https://www.github.com/Molmed/role_hercules) which can be used to configure Hercules. If you are an Ansible user we strongly encurage you to use that to configure Hercules. If not, you might want to get into the details below.

The Hercules is configured using Typesafe Config - in HOCON (a human friendly subset of json). By default the Hercules config will be found under: `/etc/hercules/application.conf`. After you have made any changes to the config you need to restart the service: `service hercules restart`. Most of the config should have reasonable defaults, but one warant a comment:

*Setting up the roles*

    hercules {
       roles = ["master", "restapi"]
    }

By default the "master" and "restapi" roles will be set. The other available roles at the moment are `demultiplexer` and `watcher`. Run `hercules --help` for a complete and updated list for the versoin that you are running on.

The rest of the configuration options should be fairly self-explanatory, set them to match your own setup.

Prepare for development
------------------------

You need to have nfs installed on your machine for the syncing to work properly. On ubuntu install it with:

    sudo apt-get install nfs-common nfs-kernel-server

This project is accompanied by a vagrant file. Once you have Vagrant and VirtualBox set up this means that you should be able to spool up a virtual test system with this command (run from the root of the project):

    vagrant up
    
To see the status of the virtual machines try:

    vagrant status
    
And to ssh into them:

    vagrant ssh <machine name>
    
You can also download a minimal test data set (provided that you have access to the correct ssh config) by running the `download_test_data.sh` script.

Install instructions
--------------------
Checkout the source and then package Hercules as a rpm (note that you need to have [sbt](http://www.scala-sbt.org/) installed):

    sbt rpm:packageBin

And then install the rpm with

    sudo yum install target/rpm/RPMS/noarch/hercules-<version>.noarch.rpm

This should create all the necesary upstart scripts so that `service hercules <command>` can be used to control the server.

