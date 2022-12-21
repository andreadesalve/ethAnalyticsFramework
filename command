
#Run experiment

java -Xmx13G -cp "lib/*" prediction.WekaTimeseriesEthPredictor -graphType G1 -timeslotSize 50000 -dataDirName /home/andreads/NetBeansProjects/EthereumTesting/dataCorrectBesu/ -timestampFile /home/andreads/NetBeansProjects/EthereumTesting/dataBesu/BlockTimeFrom0To4000000

# Filter instances with constant values 

awk -F, '{if($7!="NaN" && $3!=0) print; }' ResultG1Delta50000-50Conf7.csv > ResultG1Delta50000-50Conf7Filtered.csv 
