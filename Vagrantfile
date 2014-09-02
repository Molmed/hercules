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
    :uppmax => {
        :hostname => "milou-b",
        :ipaddress => "10.10.10.5",
        :type => "uppmax"
    },
}

#--------------------------------------
# General provisioning inline script
#--------------------------------------
$script = <<SCRIPT

echo "10.10.10.2    biotank" >> /etc/hosts
echo "10.10.10.3    biotank1" >> /etc/hosts
echo "10.10.10.4    biotank2" >> /etc/hosts
echo "10.10.10.5    milou-b.uppmax.uu.se" >> /etc/hosts

# Setup ssh keys
cp /vagrant/test_system/id_rsa* /home/vagrant/.ssh/
sudo chown vagrant:vagrant /home/vagrant/.ssh/id_rsa*
sudo chmod go-rwx /home/vagrant/.ssh/id_rsa
cat /vagrant/test_system/id_rsa.pub >> /home/vagrant/.ssh/authorized_keys

# Start by making yum faster
sudo yum install yum-plugin-fastestmirror
sudo yum upgrade 

# Install hercules prerequisites
sudo yum install -y java-1.7.0-openjdk-devel
sudo yum install -y /vagrant/test_system/sbt-0.13.5.rpm
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
sudo yum -y groupinstall "Development tools"

sudo yum install -y emacs-nox samba gnuplot PyXML ImageMagick libxslt-devel libxml2-devel ncurses-devel libtiff-devel bzip2-devel zlib-devel perl-XML-LibXML perl-XML-LibXML-Common perl-XML-NamespaceSupport perl-XML-SAX perl-XML-Simple

wget --no-clobber -P /vagrant/test_system/  http://download.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
sudo rpm -ivh epel-release-6-8.noarch.rpm

sudo yum install -y perl-PDL perl-PerlIO-gzip

sudo yum install -y perl-devel perl rsync dos2unix perl-CPAN gcc zlib-devel.x86_64 zlib.x86_64 expat-devel
curl -L http://cpanmin.us | perl - --sudo App::cpanminus

#Install the perl modules!
sudo /usr/local/bin/cpanm PerlIO::gzip
sudo /usr/local/bin/cpanm XML::Simple
sudo /usr/local/bin/cpanm MD5

#Install bcl2fastq
wget --no-clobber -P /vagrant/test_system/ ftp://webdata:webdata@ussd-ftp.illumina.com/Downloads/Software/bcl2fastq/bcl2fastq-1.8.4.tar.bz2

# Install the prerequisite software libraries
sudo yum install -y make libxslt libxslt-devel libxslt libxslt-devel ImageMagick bzip2 bzip2-devel zlib zlib-devel gcc-c++.x86_64 patc patch 

# Install bcl2fastq from source according to Illuminas instructions
export TMP=/tmp
export SOURCE=${TMP}/bcl2fastq
export BUILD=${TMP}/bcl2fastq-1.8.4-build
export INSTALL=/opt/CASAVA/1.8.4

#Download and install it
cd ${TMP}
cp /vagrant/test_system/bcl2fastq-1.8.4.tar.bz2 ${TMP}/
tar xjf bcl2fastq-1.8.4.tar.bz2

mkdir ${BUILD}
cd ${BUILD}
${SOURCE}/src/configure --prefix=${INSTALL}

make
sudo make install

#Patch it with our custom changes
cd $INSTALL
sudo patch -p1 --dry-run < /vagrant/test_system/bcl2fastq.patch && \
    sudo patch -p1 < /vagrant/test_system/bcl2fastq.patch

SCRIPT


#--------------------------------------
# biotank provisioning inline script
#--------------------------------------
$biotank_script = <<script

# mimic production folderstructure
mkdir -p /srv/samplesheet/processning/

mkdir -p /seqdata/biotank1
mkdir -p /seqdata/biotank2

chown -r vagrant:vagrant /seqdata/*
chown -r vagrant:vagrant /srv/samplesheet/processning/

mount biotank1:/seqdata/biotank1 /seqdata/biotank1
mount biotank2:/seqdata/biotank2 /seqdata/biotank2

script


#--------------------------------------
# uppmax provisioning inline script
#--------------------------------------
$uppmax_script = <<SCRIPT

mkdir -p /proj/a2009002/private/nobackup/runfolders
sudo chown -R vagrant:vagrant /proj

sudo yum -y groupinstall "Development tools"

sudo yum install -y emacs-nox samba gnuplot PyXML ImageMagick libxslt-devel libxml2-devel ncurses-devel libtiff-devel bzip2-devel zlib-devel perl-XML-LibXML perl-XML-LibXML-Common perl-XML-NamespaceSupport perl-XML-SAX perl-XML-Simple

wget --no-clobber -P /vagrant/test_system/  http://download.fedoraproject.org/pub/epel/6/x86_64/epel-release-6-8.noarch.rpm
sudo rpm -ivh epel-release-6-8.noarch.rpm

sudo yum install -y perl-PDL perl-PerlIO-gzip

sudo yum install -y perl-devel perl rsync dos2unix perl-CPAN gcc zlib-devel.x86_64 zlib.x86_64 expat-devel
curl -L http://cpanmin.us | perl - --sudo App::cpanminus

#Install the perl modules!
sudo /usr/local/bin/cpanm PerlIO::gzip
sudo /usr/local/bin/cpanm XML::Simple
sudo /usr/local/bin/cpanm MD5

#Install slurm

# First all the munge stuff
sudo yum install -y openssl-devel
wget --no-clobber -P /vagrant/test_system https://munge.googlecode.com/files/munge-0.5.11.tar.bz2
cd /vagrant/test_system
rpmbuild -tb --clean munge-0.5.11.tar.bz2
sudo rpm -ivh /root/rpmbuild/RPMS/x86_64/munge-*
dd if=/dev/urandom bs=1 count=1024 > /vagrant/test_system/munge.key 
sudo cp /vagrant/test_system/munge.key /etc/munge/munge.key
sudo service munge start

# Download the slurm source
wget --no-clobber -P /vagrant/test_system https://github.com/SchedMD/slurm/archive/slurm-14-03-7-1.tar.gz
cd /vagrant/test_system/
tar -z -x -f slurm-*
cd /vagrant/test_system/slurm-*/
./configure --enable-multiple-slurmd --enable-front-end
make
sudo make install
sudo cp /vagrant/test_system/slurm.conf /usr/local/etc/

slurmctld -c
sudo /usr/local/sbin/slurmd -c

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
                v.customize ["modifyvm", :id, "--memory", "512", "--cpus", 4]
            end

            #VM provisioning
            config.vm.provision :shell,
                :inline => $script

	    if options[:type] == "node"
                config.vm.provision :shell,
                    :inline => $node_script
            elsif options[:type] == "uppmax"
                config.vm.provision :shell,
                    :inline => $uppmax_script
            else
                config.vm.provision :shell,
                    :inline => $biotank_script
            end	        
        end
    end
end
