<?php
	$lockfile = "lock.txt";

	// Open door only if lockfile does not exists
	if(file_exists($lockfile)){
		// Do nothing
		echo "Opening Locked";
	}else{
		// Create lockfile
		echo "Locking Access\n";
		$fileHandle = fopen($lockfile, 'w') or die("Can't open file");
		// Run pythonscript which drives the servo
		// This script has to run as sudo!! Because the raspberry has to enter his IOs
		echo "Calling Script\n";
		$output = exec('echo "" | sudo -S python /var/www/secure/scripts/servo.py');
		echo "Finished Script\n";
		// Close lockfile
		echo "Closing Lockfile\n";
		fclose($fileHandle);
		// Delete lockfile
		echo "Deleting Lockfile\n";
		unlink($lockfile);
	}
?>
