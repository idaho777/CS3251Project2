from random import randint
import Queue,thread,time
import socket,sys,signal
import optparse
from copy import copy
from optparse import Option, OptionValueError
#import numpy as np

#RXINT=0.1
PSINT=0.5
DELAY=800
def signal_handler(signal, frame):
    #a=OutTime-InTime
    #np.savetxt('time.out',a,delimiter=',')
    print "Shutting down the emulator."
    sys.exit(0)

def Main():   
    sock.setblocking(0)
    try:
        thread.start_new_thread(ReceivePacket,())
        thread.start_new_thread(ProcessQueue,())
    except:
        print "Error"
    while 1:
        pass

def ReceivePacket():
    #global InTime
    while 1:
        #time.sleep(0.001)
        try:
            packet = sock.recvfrom(65535)
            #InTime=np.append(InTime,int(round(time.time()*1000)))
            data=packet[0]
            if not data: break
            if (randint(1,100)<crPb): packet=(corrupt(packet[0]),packet[1]) #todo
            elif (randint(1,100)<dupPb): queue.put(packet)
            if (randint(1,100)>dropPb):
                queue.put(packet)
                print "packet queued:"+data
            else: print "packet dropped"
        except socket.error,msg:
            continue

def corrupt(data):
    #now=int(round(time.time()*1000))
    list=split(data,8)
    data=""
    for i in xrange(0,len(list)):
        data=data+list[i][:-1]+"0"
    #print (int(round(time.time()*1000))-now)
    return data

def ProcessQueue():
    while 1:
        PSINT=randint(10,delay*2)/1000.0
        time.sleep(PSINT)
        while not queue.empty():
            packet=queue.get()
            if (randint(1,100)<rePb): 
                queue.put(packet)
                break
            print "Trying to send packet."
            send(packet)

def send(packet):
    #global OutTime
    port=packet[1][1]
    ip=packet[1][0]
    #port=port+(1 if port%2 is 0 else -1)
    sock.sendto(packet[0],(ip,
                  ((port+1) if port%2 is 0 else (port-1))
                              ))
    #OutTime=np.append(OutTime,int(round(time.time()*1000)))
    print "packet sent:"+packet[0]

def check_prob(option,opt,x):
    try:
        x=int(x)
    except ValueError:
        raise optparse.OptionValueError(
            "option %s: invalid integer value: %r" % (opt, x))
    if not(0<x<100):
        raise optparse.OptionValueError("option: %s - Probability should be between 0 and 100" %opt)
    return x

def check_ms(option,opt,x):
    try:
        x=int(x)
    except ValueError:
        raise optparse.OptionValueError(
            "option %s: invalid integer value: %r" % (opt, x))
    if not(0<x<1000):
        raise optparse.OptionValueError("option: %s - Delay should be between 0 and 1000 miliseconds" %opt)
    return x

def split(str, num):
    return [ str[start:start+num] for start in range(0, len(str), num) ]

class MyOptions (Option):
    TYPES = Option.TYPES + ("prob","ms")
    TYPE_CHECKER = copy(Option.TYPE_CHECKER)
    TYPE_CHECKER["prob"] = check_prob
    TYPE_CHECKER["ms"] = check_ms
	
if __name__ == '__main__':
    signal.signal(signal.SIGINT, signal_handler)
    queue=Queue.Queue(maxsize=15000);
    usage="usage: %prog port [-l loss_prob] [-c corruption_prob] [-d duplication_prob] [-r reordering_prob] [-D average_delay]"
    parser = optparse.OptionParser(option_class=MyOptions,usage=usage)
    parser.add_option('-l', '--loss', type="prob", dest='loss', default=0)
    parser.add_option('-c', '--corruption', type="prob", dest='corrupt', default=0)
    parser.add_option('-d', '--duplication', type="prob", dest='dup', default=0)
    parser.add_option('-r', '--reordering', type="prob", dest='reord', default=0)
    parser.add_option('-D', '--delaying', type="ms", dest='delay', default=0)
    try:
        (options, args) = parser.parse_args()
        if len(args) < 1 : parser.error("Argument port is required")
        if len(args) > 1 : parser.error("Only 1 argument is required")
        port=int(args[0])
        if not(1000<port<20000): parser.error("Port number should be between 1000 and 20000")
    except ValueError: parser.error("Port number should be a integer")
    except Exception,e: print str(e)
    print options
    options=options.__dict__
    crPb=options['corrupt']
    dupPb=options['dup']
    dropPb=options['loss']    
    rePb=options['reord']
    DELAY=options['delay']
    delay=max(DELAY,10)
    host=''
    UDP_PORT=8000
    try :
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
        print 'Socket created'
    except socket.error, msg :
        print 'Failed to create socket. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
        sys.exit()
    try:
        sock.bind((host, UDP_PORT))
        print "Listening"
    except socket.error , msg:
        print 'Bind failed. Error Code : ' + str(msg[0]) + ' Message ' + msg[1]
        sys.exit()
    #InTime=np.array([])
    #OutTime=np.array([])
    Main()
