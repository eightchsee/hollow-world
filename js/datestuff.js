function datestamp () {
	var now = new Date();
	var monthnum = now.getMonth() + 1;
	var date = now.getDate();
	var year = now.getYear();
	if (year < 2000) year = 1900 + year;
	document.write(monthnum + "-" + date + "-" + year);
}
function link2today (thismonth,thisyear) {
	var curr = new Date();
	var year = takeYear(curr);
	var monthnum = curr.getMonth() + 1;
	var month = monthnum < 10 ? '0' + monthnum : monthnum;
//	var link = month + '-' + curr.getDate() + '-' + year;
	var linkText = Day[curr.getDay()] + ' ' + Month[curr.getMonth()] + ' ' + doDate(curr.getDate()) + ', ' + year;
	if(thismonth == monthnum && thisyear == year)
	  {document.write('Today&apos;s Date: <span style=\"color: #c00000;\">' + linkText + '</span>'); return;}
//	var ref = 'cal' + year + '-' + month + '-3mo.html';
	var dir = '../' + year + '/';
	//var ref = dir + 'cal' + year + '-' + month + '.html';
	var ref = year <= 2012 ? dir + 'cal' + year + '-' + month + '.html' : dir + month + '-' + Month[curr.getMonth()].substring(0,3) + '.html'
	document.write('Today&apos;s Date: <a href=\"' + ref + '\">' + linkText + '</a>');
//	alert("dir: " + dir + "\n" + "ref: " + ref + "\n" + "linkText: " + linkText);
//	alert(dir + month + '-' + Month[curr.getMonth()].substring(0,3) + '.html');
//  http://webpages.charter.net/heibercobb/cal/2012/cal2012-12.html
//  http://webpages.charter.net/heibercobb/cal/2013/12-Dec.html
//dir: '../' + 2012 + '/'
//ref: ../2012/ + 'cal' + 2012 + '-' + 12 + '.html'
//href ../2013/ + month + '-' + Month[curr.getMonth()].substring(0,3) + '.html'
//linkText: Saturday December 8th, 2012
}
function takeYear(theDate) {
	x = theDate.getYear();
	var y = x % 100;
	y += (y < 38) ? 2000 : 1900;
	return y;
}
var Day = new Array('Sunday', 'Monday', 'Tuesday', 'Wednesday', 'Thursday', 'Friday', 'Saturday');
var Month = new Array('January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December');
function doDate(dat) {
	var theDate = dat + 1;
	if((dat >= 4 && dat <= 20) || (dat >= 24 && dat <= 30))
	  return dat + 'th';
	else if(dat == 1 || dat == 21 || dat == 31)
	  return dat + 'st';
	else if(dat == 2 || dat == 22)
	  return dat + 'nd'
	else if(dat == 3 || dat == 23)
	  return dat + 'rd'
}
// the following was made unnecessary by the "Day" array above
// but it's still worth remembering
function textDay(day) {
	switch (day)
	{
		case 0: return 'Sunday';
		case 1: return 'Monday';
		case 2: return 'Tuesday';
		case 3: return 'Wednesday';
		case 4: return 'Thursday';
		case 5: return 'Friday';
		case 6: return 'Saturday';
	}
}

function setit(m, y) {
  var elm;
  var dt = new Date();
  var mth = dt.getMonth() + 1;
  var yr = dt.getFullYear();
// alert(dt + "\n" + "  m: " + m + "\n" + "mth: " + mth + "\n" + "y: " + y + "\n" + "yr: " + yr);
  if (m == mth && y == yr) {
    var d = dt.getDate();
    //  alert(m + "-" + d + "-" + y);
    elm = document.getElementById(d);
    elm.style.color="#c00000"
    elm.style.fontSize="17pt"
  }
}

function st_holiday(dd, m, y, txt) {
  var elm;
  var dt = new Date();
  var mth = dt.getMonth() + 1;
  var yr = dt.getFullYear();
  if (y == yr) {
    for(var i = 0; i < dd.length; i++) {
      var d = parseInt(dd[i], 10);
      elm = document.getElementById(d);
      elm.style.backgroundColor="#ADD8E6";
      elm.innerHTML = d + '<p style="color: #f00; font-size: 7.5pt; font-weight: normal;">' + txt[i] +'</p>';
    }
  }
  if (m == mth && y == yr) {
    var d = dt.getDate();
    elm = document.getElementById(d);
    elm.style.color="#c00000"
    elm.style.fontSize="17pt"
  }
}

