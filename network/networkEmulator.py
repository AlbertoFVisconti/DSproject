#!/usr/bin/env python3

import sys
from mininet.topo import Topo
from mininet.net import Mininet
from mininet.node import Node
from mininet.log import setLogLevel, info
from mininet.cli import CLI
from mininet.link import TCLink
from mininet.topo import Topo
from mininet.term import makeTerm

from mininet.node import OVSSwitch

routers = {}
hosts={}
DELAY='50ms'
LOSS=0.0


def ip_to_bits(ip:str)-> str:
    octets = ip.split('.')
    bits = ''.join(f"{int(octet):08b}" for octet in octets)
    return bits

def print_help():
    print("""
    Usage:
    - To display help: use the [-h] or [--help] flags
    - To run the emulator: pass the number of hosts as an argument

    Description:
    This will start a Mininet network with:
        • A central switch
        • N routers connected to the switch
        • Each router connected to a single host
          
    Remember: 
    Mininet must run as root so run it using the command: sudo venv/bin/python networkEmulator.py N
    To install the dependecies use the command: pip install -r requirements.txt
    To modify the error rate and delay modify the global variables in this script
    """)

class LinuxRouter(Node):
    def config(self, **params):
        super(LinuxRouter, self).config(**params)
        # Enable forwarding on the router
        self.cmd('sysctl net.ipv4.ip_forward=1')

    def terminate( self ):
        self.cmd('sysctl net.ipv4.ip_forward=0')
        super(LinuxRouter, self).terminate()

class Topology(Topo):
    def build(self, **params):
        global routers, hosts, DELAY, LOSS
        hostNumber = int(params['number'])
        mininet_routers={}
        mininet_hosts={}

        # Get routers and add them to Mininet
        for i in range(1, hostNumber+1):
            router_name=f'r{i}'
            routers[router_name] = {}
            router = self.addHost(router_name, cls=LinuxRouter)
            mininet_routers[router_name]=router
            routers[router_name]['eth0']={
                "address": f'10.0.{i}.2',
                "mask": "255.255.255.252"
            }
            routers[router_name]['eth1']={
                "address": f'192.0.0.{i}',
                "mask": "255.255.255.0" 
            }

        # Get hosts and add them to Mininet
        for i in range(1,hostNumber+1):
            host_name=f'h{i}'
            hosts[host_name]={}
            host=self.addHost(host_name)
            mininet_hosts[host_name]=host 
            hosts[host_name]['eth0'] = {
                "address": f'10.0.{i}.1',
                "mask": "255.255.255.252"
            }

        #Create the central switch s1
        switch_name="s1"
        switch = self.addSwitch(switch_name)

        #Create links
        #Connect routers to switch
        for i in range(1,hostNumber+1):
            router_name=f'r{i}'
            interface_name="eth1"
            router_ip=routers[router_name][interface_name]["address"]
            router_netmask=ip_to_bits(routers[router_name][interface_name]["mask"]).count('1')
            self.addLink(switch, mininet_routers[router_name],intfName1=f's1-eth{i}', intfName2=f'{router_name}-{interface_name}', 
                         param2={'ip':f'{router_ip}/{router_netmask}'}, delay=DELAY,loss=LOSS )

        #Connect hosts and routers without switches
        for i in range(1,hostNumber+1):
            router_name=f'r{i}'
            router_interface_name="eth0"
            host_name=f'h{i}'
            host_interface_name="eth0"
            self.addLink(mininet_routers[router_name], mininet_hosts[host_name],
                        intfName1= f'{router_name}-{router_interface_name}', intfName2= f'{host_name}-{host_interface_name}')
                    

def main():
    global routers, hosts

#HELP FUNCTION
    if len(sys.argv)==2 and sys.argv[1] in ('-h', '--help'):
        print_help()
#START THE MININET EMULATOR
    elif len(sys.argv)==2:
        hostNumber = sys.argv[1]
        if(int(hostNumber)<3):
            print("You must have at least 3 host: two Client and one or more Peer")
            return
        t = Topology(number=hostNumber)
        hostNumber=int(hostNumber)
        net = Mininet(topo=t, link=TCLink)
        net.start()
        #Create host interfaces
        for host,interfaces in hosts.items():
            for interface,config in interfaces.items():
                net[host].cmd(f'ifconfig {host}-{interface} {config["address"]} netmask {config["mask"]}')
                net[host].cmd(f'ifconfig {host}-{interface} up')
        #Create router interfaces
        for router,interfaces in routers.items():
            for interface,config in interfaces.items():
                net[router].cmd(f'ifconfig {router}-{interface} {config["address"]} netmask {config["mask"]}')
                net[router].cmd(f'ifconfig {router}-{interface} up')
        #Set default routes for hosts
        for i in range(1,hostNumber+1):
            host_name= f'h{i}'
            host_interface="eth0"
            router_address= f'10.0.{i}.2'
            net[host_name].cmd(f'ip route add default via {router_address} dev {host_name}-{host_interface}')

        #Add routing for the routers
        for i in range(1,hostNumber+1):
            for j in range(1,hostNumber+1):
                if i!=j:
                    router=net[f'r{i}']
                    router.cmd(f'ip route add 10.0.{j}.0/30 via 192.0.0.{j}')
    
        #Create client for H1,H2 and the peer net 
        terminals=[]
        first_leader_number= 3 
        first_leader= net[f'h{first_leader_number}']
        terminals.append(makeTerm(first_leader, cmd="bash -c 'cd ../out/production/progetto1 && java peer.Peer 5000; exec bash'"))
        for i in range(4,hostNumber+1):
            host_name= f'h{i}'
            host=net[host_name]
            terminals.append(makeTerm(host,cmd=f"bash -c 'cd ../out/production/progetto1 && java peer.Peer 5000 10.0.{first_leader_number}.1 5000; exec bash'"))
        h1 = net['h1']
        terminals.append(makeTerm(h1, cmd=f"bash -c 'cd ../out/production/progetto1 && java client.Client 10.0.1.1 6000 10.0.{first_leader_number}.1 5000; exec bash'"))
        h2 = net['h2']
        terminals.append(makeTerm(h2, cmd=f"bash -c 'cd ../out/production/progetto1 && java client.Client 10.0.2.1 6000 10.0.{first_leader_number}.1 5000; exec bash'"))

        CLI(net)

        #Cleanup once Mininet has been quit
        for terminal in terminals:
            for proc in terminal:
                proc.terminate()
        net.stop()



    else:
        print("Pass [-h] for help message")


if __name__ == '__main__':
    main()
