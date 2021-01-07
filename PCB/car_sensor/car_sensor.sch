EESchema Schematic File Version 4
EELAYER 26 0
EELAYER END
$Descr A4 11693 8268
encoding utf-8
Sheet 1 1
Title ""
Date ""
Rev ""
Comp ""
Comment1 ""
Comment2 ""
Comment3 ""
Comment4 ""
$EndDescr
$Comp
L wemos_mini:WeMos_mini U?
U 1 1 5C4F9AC3
P 5750 2200
F 0 "U?" H 5750 2943 60  0001 C CNN
F 1 "WeMos_mini" H 5750 2731 60  0000 C CNN
F 2 "" H 6300 1500 60  0001 C CNN
F 3 "" H 5750 2731 60  0001 C CNN
	1    5750 2200
	1    0    0    -1  
$EndComp
$Comp
L power:PWR_FLAG #FLG?
U 1 1 5C594544
P 2850 1850
F 0 "#FLG?" H 2850 1925 50  0001 C CNN
F 1 "PWR_FLAG" H 2850 2024 50  0000 C CNN
F 2 "" H 2850 1850 50  0001 C CNN
F 3 "" H 2850 1850 50  0001 C CNN
	1    2850 1850
	1    0    0    -1  
$EndComp
$Comp
L power:GND #PWR?
U 1 1 5C52E918
P 5250 1950
F 0 "#PWR?" H 5250 1700 50  0001 C CNN
F 1 "GND" H 5255 1777 50  0000 C CNN
F 2 "" H 5250 1950 50  0001 C CNN
F 3 "" H 5250 1950 50  0001 C CNN
	1    5250 1950
	0    1    1    0   
$EndComp
$Comp
L Switch:SW_Push_Open SW?
U 1 1 5C938ACF
P 6450 3000
F 0 "SW?" H 6450 2810 50  0000 C CNN
F 1 "flash" H 6450 2901 50  0000 C CNN
F 2 "" H 6450 3200 50  0001 C CNN
F 3 "" H 6450 3200 50  0001 C CNN
	1    6450 3000
	-1   0    0    1   
$EndComp
Wire Wire Line
	6250 2350 6650 2350
Wire Wire Line
	6250 2550 6250 3000
Wire Wire Line
	6250 2450 7050 2450
Wire Wire Line
	6650 2350 6650 3000
Wire Wire Line
	7050 2450 7050 2950
$Comp
L NNR-lib:12-24V-1.8-12V_DCDC_converter U?
U 1 1 5DC866EE
P 4350 1150
F 0 "U?" V 4360 1438 50  0000 L CNN
F 1 "12-24V to 1.8-12V_DCDC_converter" V 4451 1438 50  0000 L CNN
F 2 "" H 4200 1000 50  0001 C CNN
F 3 "" H 4200 1000 50  0001 C CNN
	1    4350 1150
	0    1    1    0   
$EndComp
Wire Wire Line
	4250 1850 4250 1750
Text Label 4750 1850 0    50   ~ 0
5V
Wire Wire Line
	4550 1750 4550 1850
Wire Wire Line
	4100 1750 4250 1750
Connection ~ 4250 1750
Wire Wire Line
	4550 1850 5250 1850
$Comp
L power:GND #PWR?
U 1 1 5DCA3328
P 4400 1750
F 0 "#PWR?" H 4400 1500 50  0001 C CNN
F 1 "GND" H 4405 1577 50  0000 C CNN
F 2 "" H 4400 1750 50  0001 C CNN
F 3 "" H 4400 1750 50  0001 C CNN
	1    4400 1750
	1    0    0    -1  
$EndComp
Text Label 2850 1850 0    50   ~ 0
12V
$Comp
L Device:Battery_Cell BT?
U 1 1 5F572920
P 2850 2050
F 0 "BT?" H 2968 2146 50  0000 L CNN
F 1 "Battery_Cell" H 2968 2055 50  0000 L CNN
F 2 "" V 2850 2110 50  0001 C CNN
F 3 "~" V 2850 2110 50  0001 C CNN
	1    2850 2050
	1    0    0    -1  
$EndComp
$Comp
L power:GND #PWR?
U 1 1 5F572B8B
P 2850 2150
F 0 "#PWR?" H 2850 1900 50  0001 C CNN
F 1 "GND" H 2855 1977 50  0000 C CNN
F 2 "" H 2850 2150 50  0001 C CNN
F 3 "" H 2850 2150 50  0001 C CNN
	1    2850 2150
	1    0    0    -1  
$EndComp
$Comp
L Switch:SW_Push SW?
U 1 1 5F5734C2
P 3500 1850
F 0 "SW?" H 3500 2135 50  0000 C CNN
F 1 "SW_Push" H 3500 2044 50  0000 C CNN
F 2 "" H 3500 2050 50  0001 C CNN
F 3 "" H 3500 2050 50  0001 C CNN
	1    3500 1850
	1    0    0    -1  
$EndComp
Wire Wire Line
	3700 1850 4250 1850
Wire Wire Line
	3300 1850 2850 1850
Connection ~ 2850 1850
$EndSCHEMATC
