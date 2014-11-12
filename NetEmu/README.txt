Quick start:

1. Install Nmap to use a testing tool called ncat: http://nmap.org/download.html
2. run: NetEmu 8000
3. run "client" in one terminal: ncat localhost 8000 -u -p 4000
4. run "server" in another terminal: ncat localhost 8000 -u -p 4001
5. type anything in one end and check if it is arriving at the other end


Instructions to run

The command NetEmu has 1 required argument and 5 optional arguments:

<port>: 	A UDP port number is required. That will be the port where the emulator will listen for incoming packets.
-l 	Loss probability default value is 0. 
		Ex.: NetEmu 5000 -l 50, will emulate, on port 5000, a channel with 50% packet loss.
-c 	Corruption probability default value is 0. 
		Ex.: NetEmu 4000 -c 30, will emulate, on port 4000, a channel where 30% of the packets are corrupted. 
-d	Duplication probability default value is 0. 
		Ex.: NetEmu 8000 -d 25, will emulate a channel that duplicates 25% of sent packets
-D	Average Delay. 
		Ex.: NetEmu 8000 -D 100 will emulate a channel with average delay of packets in 100 ms.
-r	Reordering probability. 
		Ex.: NetEmu 800 -r 50 -D 50 will emulate a channel with packets arriving out-of-ordering with probability of 50% per burst

Details

If the delay is too small the emulator might not have bursts of packets, thus, the reordering wouldn't affect anything. to make sure the packets are out of order use the features -D and -r together.

The Functions can be applied concurrently:
NetEmu 8000 -l 10 -c 10 -d 10 -D 100 -r 25% will create a network quite messy

Platform details

On windows make sure your firewall allows the application to run and exchange traffic.

1. Open a command window on the folder of the NetEmu.exe file
2. Run NetEmu 8000

On Linux you can run it using NetEmu 8000 or python NetEmu.py 8000

On Mac run it using python NetEmu.py 8000