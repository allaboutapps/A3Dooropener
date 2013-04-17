<?php
	$lockfile = "lock.txt";

	// Open door only if lockfile does not exists
	if(file_exists($lockfile)){
		// Do nothing
	}else{
		// Create lockfile
		$fileHandle = fopen($lockfile, 'w') or die("Can't open file");
		// Run pythonscript which drives the servo
		// This script has to run as sudo!! Because the raspberry has to enter his IOs
		$output = exec('echo "" | sudo -S python /var/www/secure/scripts/servo.py');
		// Close lockfile
		fclose($fileHandle);
		// Delete lockfile
		unlink($lockfile);
	}
?>
