#Define the list of machines
biotanks = {
    :biotank1 => {                                                              
        :hostname => "biotank1",
        :ipaddress => "10.10.10.3",
        :type => "node"
    },
    :biotank2 => {
        :hostname => "biotank2",
        :ipaddress => "10.10.10.4",
        :type => "node"
    },
    :biotank => {
        :hostname => "biotank",
        :ipaddress => "10.10.10.2",
        :type => "biotank"
    },
}

#--------------------------------------
# General provisioning inline script
#--------------------------------------
$script = <<SCRIPT

echo "10.10.10.2    biotank" >> /etc/hosts
echo "10.10.10.3    biotank1" >> /etc/hosts
echo "10.10.10.4    biotank2" >> /etc/hosts

# Setup ssh keys
cp /vagrant/id_rsa* /home/vagrant/.ssh/
sudo chown vagrant:vagrant /home/vagrant/.ssh/id_rsa*
cat /vagrant/id_rsa.pub >> /home/vagrant/.ssh/authorized_keys

# Install hercules prerequisites
sudo yum install -y java-1.7.0-openjdk-devel
sudo yum install -y /vagrant/sbt-0.13.5.rpm

# Install the nfs stuff
yum install -y nfs-utils nfs-utils-lib
chkconfig nfs on 
service rpcbind start
service nfs start

SCRIPT


#--------------------------------------
# Node provisioning inline script
#--------------------------------------
$node_script = <<SCRIPT

mkdir -p /seqdata/$(hostname)/runfolders
chown -R vagrant:vagrant /seqdata/*

echo "/seqdata/$(hostname) 10.10.10.2(rw,no_root_squash)" > /etc/exports
sudo exportfs -a

SCRIPT


#--------------------------------------
# Biotank provisioning inline script
#--------------------------------------
$biotank_script = <<SCRIPT

# Mimic production folderstructure
mkdir -p /srv/samplesheet/Processning/

mkdir -p /seqdata/biotank1
mkdir -p /seqdata/biotank2

chown -R vagrant:vagrant /seqdata/*

mount biotank1:/seqdata/biotank1 /seqdata/biotank1
mount biotank2:/seqdata/biotank2 /seqdata/biotank2

SCRIPT

#--------------------------------------
# Fire up the machines
#--------------------------------------
Vagrant.configure("2") do |global_config|
    biotanks.each_pair do |name, options|
        global_config.vm.define name do |config|
            #VM configurations
            config.vm.box = "chef/centos-6.5"
            config.vm.hostname = "#{name}"
            config.vm.network :private_network, ip: options[:ipaddress]

            #VM specifications
            config.vm.provider :virtualbox do |v|
                v.customize ["modifyvm", :id, "--memory", "512"]
            end

            #VM provisioning
            config.vm.provision :shell,
                :inline => $script

	    if options[:type] == "node"
                config.vm.provision :shell,
                    :inline => $node_script	        
            else
                config.vm.provision :shell,
                    :inline => $biotank_script
            end	        
        end
    end
end
