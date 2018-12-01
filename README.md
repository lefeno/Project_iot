Link Firebase:
https://fir-testing-e07e2.firebaseio.com/
### Reference
MQTT:
- https://wildanmsyah.wordpress.com/2017/05/11/mqtt-android-client-tutorial/?fbclid=IwAR0Uv5VfrScR6UdLh8EAzQLoveLaBAkpuX8LJdIevCZJTHqT4Fp0an0sLvU

FIREBASE:
- https://firebase.google.com/docs/database/android/start/

BLE:
- https://stackoverflow.com/questions/9231598/how-to-read-all-bytes-together-via-bluetooth
- https://www.instructables.com/id/Android-Bluetooth-Control-LED-Part-2/
- https://stackoverflow.com/questions/32656510/register-broadcast-receiver-dynamically-does-not-work-bluetoothdevice-action-f
- https://developer.android.com/guide/topics/connectivity/bluetooth#java
- https://viblo.asia/p/tim-hieu-ve-bluetooth-api-tren-android-tao-mot-ung-dung-bluetooth-scanner-3wjAM7JARmWe
- https://stackoverflow.com/questions/15025852/how-to-move-bluetooth-activity-into-a-service
- https://developer.android.com/guide/components/bound-services#java

DB Types of plant (Max, min parameters based on Internet)

	plant{
		0{
			id: 0
			name: Cress	//Rau mam
			humid_max: 95
			humid_min: 80
		}

		1{
			id: 1
			name: Succulent	//Sen da
			humid_max: 70
			humid_min: 40
			}
		}

		2{
			id: 2
			name: Catus //Xuong rong
			humid_max: 70
			humid_min: 20
		}
				
	}

Database Devices

	db{
		user0{
			mac0{
				pot1{
					type: 0
					auto: true
					humid_max: 95 
					humid_min: 80
					commands{
          					key0
	        	  				id: key0
        					      	value: 123
							time: dd/mm/yyy hh:mm:ss
       	  					key1
         						id: key1	
							value: 456
							time: dd/mm/yyy hh:mm:ss
				        }
			        	logs{
		        		  	key2
				              		id: key2
					              	value: 911
							time: dd/mm/yyy hh:mm:ss
						key3
							id: key3
				             	 	value 905
							time: dd/mm/yyy hh:mm:ss
			        	}
			        	data{
					        key4
			              			id: key4
				              		value: 234
							time: dd/mm/yyy hh:mm:ss
				          	key5
				              		id: key5
				              		value: 345
							time: dd/mm/yyy hh:mm:ss
			        	}
				}
				pot2{
					type: 1
					auto: false
					humid_max: 70 
					humid_min: 40
				}
			}
 			mac1{
				pot1{
				}
				pot2{
				}
			}
		}

		user1{
		}
	}
Each BLE have unique MAC address -> represent an area.
- 1 area have many pot, max 5 pot (1->5), can contains different kind of plants.
Current is 2 pots only. 
- Default auto = false.

## COMMUNICATION CODE

### Notice
- y: pot yth (1<=y<=5) 
- xx: humidity (00<=xx<=99)
- z: MAC address (17 chars)
- DB: MQTT, FB

### CONTROL
- Byx1x2x3x4z: App -> DB -> Pi, auto water pot y only with max humidity x1x2%, min humidity x3x4%
- If auto water all pots, for each current pot, send Byxxz
- Cyxxz: App -> DB -> Pi, water with xx% humidity, xx is maximum humidity that requires for the plant at pot y
- yxx: Pi -> Device

- Ry00z: App -> DB -> Pi, self water pot y only
- If self water all pots, for each current pot, send Ry00z

### DATA
- yxx: Device -> Pi
- Dyxxz: Pi -> DB, send data to mqtt and firebase, xx is humidity at pot y

### LOG
- yEE: Device -> Pi
- Ey00z: Pi -> DB
- Meaning: water successfully at pot y

- yFF: Device -> Pi, not used now
- Fy00z: Pi -> DB, not used now
- Meaning: water error at pot y

- G000z: Pi -> DB, not used now
- Meaning: BLE MAC error connection

- yHH: Device -> Pi
- Hy00z: Pi -> DB
- Meaning: Pot y at BLE MAC error connection

MQTT have channels:
- command: read only
- log: write only
