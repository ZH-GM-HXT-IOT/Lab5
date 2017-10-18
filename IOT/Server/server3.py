import network
import time
import ujson
import ssd1306
import socket
import machine
import urequests
from machine import RTC




html1= """<!DOCTYPE html>
<html>
    <head><title>200 OK</title></head>
    <body>
    <h1>Not found</h1>
    <p>The requested URL was not found</p>
    </body>
</html>
"""


def do_connect():
   
    sta_if = network.WLAN(network.STA_IF)
    if not sta_if.isconnected():
        print('connecting to network...')
        sta_if.active(True)
        sta_if.connect('Columbia University', ' ')
        while not sta_if.isconnected():
            pass
    print('network config:', sta_if.ifconfig())

def resolve_instr(res):
    global html1
    c1=res[0]
    addr=res[1]
    print('client connected from', addr)
    print('client socket', c1)
    print('request')
    req=c1.recv(4096)
    print(req)
    html=req
    html=html.split(b'\r\n\r\n') #converts it into list
        #_[-1]
    data=html[-1] #gets unsplited data
    data1=data.decode("utf-8") #byte to string conversion
    data2=data1.split("=",1) #split it using "=" sign
    data4=data2[1]
    data3=data2[0]
    data5=" "
    print(data3)
    print(data4)
    data5=data4
    data6=data5.replace("+"," ")
    return data3,c1,addr,data6

def screenshow(oled,text,col,row,add = None,more = None):
    if add == None:
        oled.fill(0)
    textNum = 20
    textheight = 10
    cur_pt = 0
    while cur_pt < len(text):
        oled.text(text[cur_pt:cur_pt + textNum],col,row)
        cur_pt += textNum
        row += textheight
    if more == None:
        oled.show()

def screendark(oled):
    oled.fill(0)
    oled.show()

def screenlight(oled):
    oled.fill(1)
    oled.show()

def fetchdate(rtc):
    L=rtc.datetime()

    ymd=(rtc.datetime()[0],rtc.datetime()[1],rtc.datetime()[2])
    hms=(rtc.datetime()[4],rtc.datetime()[5],rtc.datetime()[6])
    ymd=str(ymd)
    hms=str(hms)
    return ymd,hms

def get_location():     #the return value of this function is float type, some type casting is needed in next usage
    googleurl = r'https://www.googleapis.com/geolocation/v1/geolocate?key=AIzaSyDSuuz0YCx3DNGGvblrwXtjjlzrZaR3j2k'
    f = open('googlemap.json')    #googlemap.json is a file we have put to the board before
    requestdata = f.read()   
    f.close()
    response = urequests.post(googleurl,data = requestdata)
    loc_josn = response.json()
    loc_josn = loc_josn['location']
    return [loc_josn['lat'],loc_josn['lng']]

def get_weather():
    location = get_location()
    #the return value of this function is float type, some type casting is needed in next usage
    location = [int(var) for var in location]
    #print(get_weather(location_int))
    
    weatherurl = "http://api.openweathermap.org/data/2.5/weather?lat={0:d}&lon={1:d}&APPID=c9854c1788404511ef45ac54c8a21def".format(location[0],location[1])
    response = urequests.post(weatherurl)
    response_str = response.json()
    weather_str = response_str['weather']
    weather_str = weather_str[0]
    return weather_str['main']

##################### init ###########################
do_connect()

addr= socket.getaddrinfo('0.0.0.0', 80)[0][-1]
s=socket.socket()
s.bind(addr)


rtc=RTC()
rtc.datetime((2017,10,16,1,21,15,34,300))

i2c = machine.I2C(scl=machine.Pin(5), sda=machine.Pin(4))
oled = ssd1306.SSD1306_I2C(128, 32, i2c)

s.listen(1)
print('listening on', addr)
#from ssd1306a import SSD1306 as ssd
#d=ssd()




time_flag= False

#screenlight(oled)
s.settimeout(0.5)

while True:
    L=rtc.datetime()
    #print(L)
    try:
        res=s.accept()
    except OSError:
        #print("Nothing")
        pass
    else:
        instruction,cp,add,messages = resolve_instr(res)
        print(instruction)
        success=" "
        if instruction=="DisplayON":
            screenshow(oled,"Watch On",0,0)
            print("DisplayON")
        if instruction=="DisplayTime":
            time_flag=True
            print("DisplayTime")
        if instruction=="message":    #max to max 21 characters
            screenshow(oled,messages,0,0)
            print(messages)
            time_flag= False
        if instruction=="DisplayOFF":
            screendark(oled)
            time_flag= False
            print("DisplayOFF")
        if instruction == 'Weather':
            weather = get_weather()
            screenshow(oled,weather,0,0)
            print("display Weather")
            time_flag= False
            
            
        resp12= "HTTP/1.1 200 {0:s}\r\nContent-Type:".format(instruction)
        resp12 += "application/text\r\nContent-Length: 10\r\n\r\n{'resp111re111234'}"
        cp.send(resp12)
        print("reply sent......")
        time.sleep(1)
        cp.close()
        
    if time_flag==True:
        ymd,hms = fetchdate(rtc)
        screenshow(oled,ymd,0,0,more = True)
        screenshow(oled,hms,0,10,add = True)
