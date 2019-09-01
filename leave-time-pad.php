<?php
  $curr_date = time();
  $display_curr_date = date('l', $curr_date) . ' ' . date('F', $curr_date) . ' ' . date('jS', $curr_date) . ', ' . date('Y', $curr_date);
  $display_curr_time = (date('h', $curr_date)) . ':' . date('i', $curr_date) . ':' . date('s', $curr_date) . ':' . date('A', $curr_date) . ' ' . date('e', $curr_date);
  $curr_year = date('Y', $curr_date);
?>
<!doctype html />
<html>
  <head>
    <meta content="7200" http-equiv="refresh">
    <title>my leave balances</title>
    <style type="text/css">
      body {font-size: 0.9em; margin: 25px auto auto 100px;}
      curr_time {
        display: block;
        margin-bottom: 10px;
        color: #c00000;
        font-size: small;
        line-height: 1pt;
        font-style: italic;
      }
    </style>
  </head>
  <body>
    <curr_time><?php echo $display_curr_date ?> @ <?php echo $display_curr_time ?></curr_time>
<?php
  $file_loc = './' . $curr_year . '/leave-time-pad.txt';
  $myfile = fopen($file_loc, "r") or die("Unable to open file!");
?>
    <pre>
<?php
  // Output one line until end-of-file
  while(!feof($myfile)) {
    $line = fgets($myfile);
    echo $line;
  }
  fclose($myfile);
?>
    </pre>
  <body>
</html>
