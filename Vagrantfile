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
sudo chmod go-rwx /home/vagrant/.ssh/id_rsa
cat /vagrant/id_rsa.pub >> /home/vagrant/.ssh/authorized_keys

# Install hercules prerequisites
sudo yum install -y java-1.7.0-openjdk-devel
sudo yum install -y /vagrant/sbt-0.13.5.rpm
sudo yum install -y git

# Install the nfs stuff
yum install -y nfs-utils nfs-utils-lib git
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

#Install sisyphus requirements
sudo yum install -y perl-devel perl rsync dos2unix perl-CPAN gcc zlib-devel.x86_64 zlib.x86_64 expat-devel
curl -L http://cpanmin.us | perl - --sudo App::cpanminus


#Install the perl modules!
sudo /usr/local/bin/cpanm PerlIO::gzip
sudo /usr/local/bin/cpanm XML::Simple
sudo /usr/local/bin/cpanm MD5

#Install bcl2fastq
wget --no-clobber -P /vagrant/ ftp://webdata:webdata@ussd-ftp.illumina.com/Downloads/Software/bcl2fastq/bcl2fastq-1.8.4.tar.bz2

# Install the prerequisite software libraries
sudo yum install -y make libxslt libxslt-devel libxslt libxslt-devel ImageMagick bzip2 bzip2-devel zlib zlib-devel gcc-c++.x86_64 patc patch 

# Install bcl2fastq from source according to Illuminas instructions
export TMP=/tmp
export SOURCE=${TMP}/bcl2fastq
export BUILD=${TMP}/bcl2fastq-1.8.4-build
export INSTALL=/usr/local/bcl2fastq-1.8.4

#Download and install it
cd ${TMP}
cp /vagrant/bcl2fastq-1.8.4.tar.bz2 ${TMP}/
tar xjf bcl2fastq-1.8.4.tar.bz2

mkdir ${BUILD}
cd ${BUILD}
${SOURCE}/src/configure --prefix=${INSTALL}

make
sudo make install

#Patch it with our custom changes
cd /usr/local/bcl2fastq-1.8.4/
patch -p1 --dry-run < /vagrant/CASAVA/bcl2fastq.patch && \
    patch -p1 < /vagrant/CASAVA/bcl2fastq.patch

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
chown -R vagrant:vagrant /srv/samplesheet/Processning/

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
