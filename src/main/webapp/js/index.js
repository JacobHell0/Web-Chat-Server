let ws;

//send image stuff
function sendImage() {
    //get image and store it
    let img = document.getElementById("input_button");
    if(img.files.length === 0) {
        console.log("img empty");
    } else {
        // console.log("img:");
        // console.log(img.files[0]);
        // // ws.send("test");
        //
        // //json stringify the file, to do this, make an object and stringify that
        // let file = img.files[0];
        // let object = {
        //     'lastModified' : file.lastModified,
        //     'lastModifiedDate' : file.lastModifiedDate,
        //     'name' : file.name,
        //     'size' : file.size,
        //     'type' : file.size,
        // };
        // let serialize = "";
        // const file_reader = new FileReader();
        // file_reader.writeText(file).then(r => serialize);
        // console.log(serialize);
        // let blob;
        // file_reader.readAsText(blob);
        //
        // let jsonfile = JSON.stringify(object)
        // console.log(jsonfile);
        // ws.send(jsonfile);

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

function newRoom() {
    // calling the ChatServlet to retrieve a new room ID
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
    // calling the ChatServlet to retrieve a new room ID
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

function clearTable(tableRef) {
    //get all tr elements
    let trs = tableRef.querySelectorAll("tr");

    //for each loop to remove children
    trs.forEach((item) => {
        tableRef.removeChild(item);
    });
}

function refreshList(room_list) {
    console.log("room_list: ");
    console.log(room_list);
    let tableRef = document.getElementById("refresh-list-body");
    clearTable(tableRef); //clear table so we have one that refreshes
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

    // refresh the list of rooms //TODO: jacob here: I assume we need to have an active list of rooms

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/" + code);
    console.log("room code: " + code)

    ws.onmessage = function (event) {
        let message = JSON.parse(event.data);
        console.log(event.data);
        switch(message.type) {
            case "user":
                addTable(1,message.message + " [" + timestamp() + "] " + "\n ");
                break;
            case "other":
                addTable(0,"[" + timestamp() + "] " + message.message + "\n");
                break;
            case "ChatHistory":
                //check for image
                console.log("msg123: " + message.message.substring(0, 15));
                if(message.message.substring(0, 15) === "data:image/png;") {
                    addImageToTable(0, message.message);
                } else {
                    addTable(0,"[hist] " + message.message + "\n");
                }
                break;
            case "user-image":
                addImageToTable(1, message.message);
                break;
            case "other-image":
                addImageToTable(0, message.message);
                break;
            default:
                console.log("type: " + message.type + ", msg: " + message.message);

        }

        // document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }

    document.getElementById("input").addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            let request = { "type": "chat", "msg": event.target.value };
            ws.send(JSON.stringify(request));

            event.target.value = "";
        }
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

function addTable(column, text)
{
    let table = document.getElementById('message_area')
    let newRow = document.createElement("tr")

    let cell1 = document.createElement("td");
    let cell2 = document.createElement("td");

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



    newRow.appendChild(cell1)
    newRow.appendChild(cell2)
    table.appendChild(newRow);


}





