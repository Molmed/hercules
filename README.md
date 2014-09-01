Hercules
========

The amazing Hercules project, taking the stone out of Sisyphus hands! (A real project description should be inserted here at some point)

Prepare for development
------------------------

This project is accompanied by a vagrant file. Once you have Vagrant and VirtualBox set up this means that you should be able to spool up a virtual test system with this command (run from the root of the project):

    vagrant up
    
To see the status of the virtual machines try:

    vagrant status
    
And to ssh into them:

    vagrant ssh <machine name>
    
You can also download a minimal test data set (provided that you have access to the correct ssh config) by running the `download_test_data.sh` script.

