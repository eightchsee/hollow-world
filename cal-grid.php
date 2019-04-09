<?php 
  $days_counts = array(0=>31,1=>28,2=>31,3=>30,4=>31,5=>30,6=>31,7=>31,8=>30,9=>31,10=>30,11=>31);
  $day_labels = array("Sun","Mon","Tue","Wed","Thu","Fri","Sat");

  $mo = date('m');
  $yr = date('Y');
  if ( isset($_GET['mth']) ) {
    $mo = $_GET['mth'];
  }
  if ( isset($_GET['yyyy']) ) {
    $yr = $_GET['yyyy'];
  }
  function check_leap_year($d) {
    if(date('L', $d) == 1) {return 'yes';}
    else {return 'no';}
  }
  $date = mktime(0,0,0, $mo, 1, $yr);
  if (check_leap_year($date) == 'yes') {
    $days_counts[1] = 29;
  }
  //This puts the day, month, and year in seperate variables 
  $day = date('d', $date) ; 
  $month = date('m', $date) ; 
  $year = date('Y', $date) ;
  //Here we generate the first day of the month 
  $first_day = mktime(0,0,0,$month, 1, $year) ;
  //This gets us the month name 
  $month_name = date('F', $first_day) ;
  //Once we know what day of the week it falls on, we know how many blank days occure before it. If the first day of the week is a Sunday then it would be zero
  $blank = date('w', $first_day);
  //We then determine how many days are in the current month
  $days_in_month = cal_days_in_month(0, $month, $year) ;
?>  
<!DOCTYPE html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta content="7200" http-equiv="refresh">
    <title><?php echo $month_name . ' - ' . $year; ?></title>
    <link href="css/php-cal.css" type="text/css" rel="stylesheet" />
    <link href="css/grid-samp.css" rel="stylesheet">
    <link href="css/cal-grid-samp.css" rel="stylesheet">
    <script src="js/datestuff.js" type="text/javascript"></script>
    <style type="text/css">
      div.day-label {
        background-color: #ffffc0;
        font-size: 9pt;
        border: 1px solid rgb(255,120,0);
        height: auto;
        text-align: center;
      }
      div[class~="weekend"] {
        background-color: lightgrey; /* #ffd8a8; */
      }
	    curr_time {
        display: block;
        text-align: center;
        margin: 10px;
        color: #c00000;
        font-size: small;
        line-height: 1pt;
        font-style: italic;
	    }
      #month-id:before {content: '<?php echo date('F') . ' ' ?>';}
      #month-id::after {content: '<?php echo $yr ?>';}
    </style>
  </head>
  <body onload="setit(<?php echo $mo . ', ' . $yr ?>)">
<?php
  $file = $yr . "/state-holidays.txt";
  $stateholidays = file_get_contents($file);

?>
    <div id="month-id"></div>
    <div class="wrapper">
<?php
  foreach ($day_labels as $day_label) {
?>
      <div class="day-label"><?php echo $day_label ?></div>
<?php
  }
  //This counts the days in the week, up to 7
  $day_count = 1;
  //first we take care of those blank days
  $prev_mo = $mo - 1;
  if ($prev_mo == 0) {$prev_mo = 12;}
  $left_over_prev_days_start = ($days_counts[$prev_mo - 1] - $blank) +1;
  while ( $blank > 0 ) {
  $blank--;
  $day_count++;
?>
      <div class="pre-post<?php if ($day_count == 2) {echo ' weekend';} ?>"><?php echo $left_over_prev_days_start; ?></div>
<?php 
   $left_over_prev_days_start++;
  } 
  //sets the first day of the month to 1 
  $day_num = 1;
  //count up the days, until we've done all of them in the month
  while ( $day_num <= $days_in_month ) {
    if ($day_count == 7) {
?>
      <div id="<?php echo $day_num; ?>" class="weekend"><?php echo $day_num; ?></div>
<?php 
  } // if ($day_count == 7)
  else {
     // $pay_day = explode(',', $pay_days[$mo - 1]);
?>
      <div id="<?php echo $day_num; ?>"<?php if ($day_count == 1) {echo ' class="weekend"';} ?>><?php echo $day_num; ?></div>
<?php
  }
  $day_num++; 
  $day_count++;
  //Make sure we start a new row every week
  if ($day_count > 7) {
    $day_count = 1;
  }
} // while ( $day_num <= $days_in_month )
  //Finaly we finish out the table with some blank details if needed
  $next_month_day = 1;
  while ( $day_count > 1 && $day_count <= 7 ) {
?>
      <div class="pre-post<?php if ($day_count == 7) {echo ' weekend';} ?>"><?php echo $next_month_day; ?></div>
<?php
  $day_count++;
  $next_month_day++;
  }
?>
    </div>
<?php
  $curr_date = time();
  $display_curr_date = date('l', $curr_date) . ' ' . date('F', $curr_date) . ' ' . date('jS', $curr_date) . ', ' . date('Y', $curr_date);
  $display_curr_time = (date('h', $curr_date)) . ':' . date('i', $curr_date) . ':' . date('s', $curr_date) . ':' . date('A', $curr_date) . ' ' . date('e', $curr_date);
?>
    <curr_time><?php echo $display_curr_date ?> @ <?php echo $display_curr_time ?></curr_time>
    <p>
      <a href="<?php echo $_SERVER['PHP_SELF'] ?>">home page</a> <?php echo ' [' . $_SERVER['QUERY_STRING'] . '] - [' . $_SERVER['SCRIPT_FILENAME'] . '] - [' . $_SERVER['SCRIPT_NAME'] . ']'; ?>
    </p>
  </body>
</html>
