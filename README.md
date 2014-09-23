Hercules
========

*NOTE:* This this project is under heavy development and anything can change without previous notice (and a lot of the functionality is not in place yet).

The amazing Hercules project, taking the stone out of Sisyphus hands! (A real project description should be inserted here at some point)

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

Install instructions (extremely preliminary)
--------------------------------------------
To package Hercules as a rpm:

    sbt rpm:packageBin

And then install the rpm with

    sudo yum install target/rpm/RPMS/noarch/hercules-<version>.noarch.rpm

This should create all the necesary upstart scripts so that `service hercules <command>` can be used to control the server.

For debian based systems this is slightly more complicated:

   sbt debian:packageBin

And then:

    dpkg -i target/hercules-<version>.deb

to install the debian file. Unfortuneatly the Upstart system scripts does not seem to work, so SystemV needs to be used to start the service. So use:

    sudo /etc/init.d/hercules <command>

To control the service.


