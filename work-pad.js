
const peopleNum = [1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,
                   21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,
                   41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,
                   61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,
                   81]

let people_length = peopleNum.length; // 82;
console.log("total # people: " + peopleNum.length);

let batch1 = peopleNum.length % 4 == 0 ?
             peopleNum.length / 4 :
             Math.ceil(peopleNum.length / 4);

let threeQtrs = peopleNum.length - batch1; // % 4 == 0 ?
//                peopleNum.length - (peopleNum.length / 4) :
//                peopleNum.length - (Math.ceil(peopleNum.length / 4));
console.log("threeQtrs: " + threeQtrs);
// console.log("threeQtrs: " + (peopleNum.length - batch1));
console.log(threeQtrs % 3);
console.log(Math.ceil(threeQtrs / 3));
let twoQtrs = threeQtrs % 3 == 0 ?
              threeQtrs - (threeQtrs / 3) :
              threeQtrs - (Math.ceil(threeQtrs / 3));
console.log("twoQtrs: " + twoQtrs);

console.log("batch1: " + batch1);

let batch2 = threeQtrs - twoQtrs;
console.log("batch2: " + batch2);

let batch3 = twoQtrs % 2 == 0 ?
             twoQtrs / 2 :
             Math.ceil(twoQtrs / 2);
console.log("batch3: " + batch3);

let batch4 = peopleNum.length - (batch1+batch2+batch3);
console.log("batch4: " + batch4);

console.log(peopleNum.slice(0,batch1));
console.log(peopleNum.slice(batch1,(batch1+batch2)));
console.log(peopleNum.slice((batch1+batch2),(peopleNum.length - batch4)));
console.log(peopleNum.slice((peopleNum.length - batch4)));

// peopleNum.slice(0, batch1).map(num => processNum(num).join(''));
//                   petsData.map(petTemplate).join('')
// peopleNum.slice(0, Math.round(batch1 / 2)).map(num => processNum(num)).join('');
// function processNum(num) {
//   console.log("The number is: " + num);
// }

// let group = peopleNum.slice(0, batch1); // Math.round(batch1 / 2));
let group = peopleNum.slice(0, Math.ceil(peopleNum.length / 3));
// document.getElementById('test').innerHTML = `${group.map(listGroup).join('')}`
// document.getElementById('test').innerHTML = `${group.map(num => `<li><span class="count"></span>item number: ${num}</li>`).join('')}`
document.getElementById('test').innerHTML = `
  ${group.map(num => `<li><span class="count"></span>item number squared: ${listGroup(num)}</li>`).join('')}
`
function listGroup(item) {
  let num = (item * item);
  if (num < 10) {
    num = '&nbsp;&nbsp;' + num;
  }
  else if (num < 100) {
    num = '&nbsp;' + num;
  }
  // return `<li><span class="count"></span>item number: ${num}</li>`
  return `${num}`
}
// ,
// {
//   "nameF": "Xavier",
//   "nameL": "Cougat",
//   "phone": null,
//   "loc": 5
// },
// {
//   "nameF": "Zeus",
//   "nameL": "Caboose",
//   "phone": null,
//   "loc": 5
// }
