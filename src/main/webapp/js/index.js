let ws;

//quick note, some of these functions got copied from another file by Ryan
//so there signatures will be BigGuyAtOTU, everyone helped make the javascript

//send image stuff
function sendImage() {
    //get image and store it
    let img = document.getElementById("input_button");
    if(img.files.length === 0) {
        console.log("img empty");
    } else {

        let file = img.files[0];
        const file_reader = new FileReader();
        //to get the file_reader to send things to the websocket we can add a listener

        file_reader.onload = function(event) {
            const data = event.target.result; //get the file
            console.log("sending data");
            ws.send(data);
            console.log("data sent");
        };
        file_reader.readAsDataURL(file);

    }

}

//logic to make a new room
function newRoom() {
    // get request to the ChatServlet to retrieve a new randomized room ID
    let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',

        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response)); // enter the room with the code
}

function getRoomList() {
    // calling the ChatServlet to get/refresh the current room list
    console.log("making request...");
    let callURL = "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet/room_list";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Content-Type': 'text/plain'
        },
    })
        .then(response => response.json())
        .then(response => refreshList(response)); // enter the room with the code
}

//clears the table that stores the current rooms
function clearTable(tableRef) {
    //get all tr elements
    let trs = tableRef.querySelectorAll("tr");

    //for each loop to remove children
    trs.forEach((item) => {
        tableRef.removeChild(item);
    });
}

function refreshList(room_list) {
    //logic to refresh room list
    let tableRef = document.getElementById("refresh-list-body");
    clearTable(tableRef); //clear table so we have one that refreshes
    //append the entries of the received room_list
    for (let i = 0; i < room_list.length; i++) {
        let row = document.createElement("tr");
        row.id = "tr_to_remove";
        let cell = document.createElement("td");
        let button_to_append = document.createElement("button");
        button_to_append.textContent = room_list[i];
        button_to_append.onclick = function () { enterRoom(room_list[i]); }; //ripped straight from lab8/9

        //append to the tr and td elements
        cell.appendChild(button_to_append);
        row.appendChild(cell);
        tableRef.appendChild(row);
    }
}

function enterRoom(code) {

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/" + code);
    console.log("room code: " + code)

    //function that is called when receiving a message from backend
    ws.onmessage = function (event) {
        let message = JSON.parse(event.data);
        console.log(event.data);
        switch(message.type) {
            case "user": //the user sent the message
                addTable(1,"[" + timestamp() + "] " + message.message + "\n");
                break;
            case "other": //the server or another user sent the message
                addTable(0,"[" + timestamp() + "] " + message.message + "\n");
                break;
            case "ChatHistory": //the backend sent the history
                //check for image
                console.log("msg123: " + message.message.substring(0, 15));
                if(message.message.substring(0, 15) === "data:image/png;") { //check if hist is an image
                    addImageToTable(0, message.message);
                } else {
                    addTable(0,"[hist] " + message.message + "\n");
                }
                break;
            case "user-image": //the user sent an image
                addImageToTable(1, message.message);
                break;
            case "other-image": //the server or another user sends an image
                addImageToTable(0, message.message);
                break;
            default: //just log the message, never called
                console.log("type: " + message.type + ", msg: " + message.message);

        }
    }

    //add event listener to the enter key
    document.getElementById("input").addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            let request = { "type": "chat", "msg": event.target.value };
            ws.send(JSON.stringify(request));
            event.target.value = "";
        }
    });

    //add that same listener to the send button
    document.getElementById("send_button").addEventListener("click", function () {
        let msg = document.getElementById("input");
        console.log("making request");
        console.log(msg);
        let request = { "type": "chat", "msg": msg.value};
            ws.send(JSON.stringify(request));
            msg.value = "";
            console.log("request made: " + msg.value);
    });
}

//credit to the class activity for the timestamp function
function timestamp()
{
    let d = new Date(), minutes = d.getMinutes();
    if (minutes < 10) minutes = '0' + minutes;
    return d.getHours() + ':' + minutes;
}

// ------------------------------------- SIDE BAR -------------------------------------

/* Set the width of the sidebar to 250px and the left margin of the page content to 250px */
function openNav() {
    document.getElementById("mySidebar").style.width = "250px";
    document.getElementById("main").style.marginLeft = "250px";
}

/* Set the width of the sidebar to 0 and the left margin of the page content to 0 */
function closeNav() {
    document.getElementById("mySidebar").style.width = "0";
    document.getElementById("main").style.marginLeft = "0";
}

// ------------------------------------- SIDE BAR -------------------------------------

//logic for adding an image to the table, essentially the same as addTable()
function addImageToTable(column, base64) {
    let table = document.getElementById('message_area')
    let newRow = document.createElement("tr")

    let cell1 = document.createElement("td");
    let cell2 = document.createElement("td");

    cell1.id = "other";
    cell2.id = "user";

    cell1.class = "chatbox";
    cell2.class = "chatbox";

    //create image
    let img = document.createElement("img");
    img.src = base64;

    //left side, column = 0
    if(!column) {
        cell1.appendChild(img);
    } else { //right side, column = 1
        cell2.appendChild(img);
    }

    newRow.appendChild(cell1)
    newRow.appendChild(cell2)
    table.appendChild(newRow);
}

//logic for displaying messages in the table
function addTable(column, text)
{
    //grab and create elements
    let table = document.getElementById('message_area')
    let newRow = document.createElement("tr")

    let cell1 = document.createElement("td");
    let cell2 = document.createElement("td");

    //set id and class
    cell1.id = "other";
    cell2.id = "user";

    cell1.class = "chatbox";
    cell2.class = "chatbox";

    //left side, column = 0
    if(!column) {
        cell1.textContent = text;
        cell1.style.backgroundColor = "grey";
    }
    else
    { //right side, column = 1
        cell2.textContent = text;
        cell2.style.backgroundColor = "#467eff";

    }

    //append children to the DOM
    newRow.appendChild(cell1)
    newRow.appendChild(cell2)
    table.appendChild(newRow);

}
