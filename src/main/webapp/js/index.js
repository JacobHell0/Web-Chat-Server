let ws;

function newRoom(){
    // calling the ChatServlet to retrieve a new room ID
    let callURL= "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'text/plain',

        },
    })
        .then(response => response.text())
        .then(response => enterRoom(response)); // enter the room with the code
}

function getRoomList(){
    // calling the ChatServlet to retrieve a new room ID
    console.log("making request...");
    let callURL= "http://localhost:8080/WSChatServer-1.0-SNAPSHOT/chat-servlet/room_list";
    fetch(callURL, {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'Content-Type' : 'text/plain'
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
    for(let i = 0; i < room_list.length; i++) {
        let row = document.createElement("tr");
        row.id = "tr_to_remove";
        let cell = document.createElement("td");
        let button_to_append = document.createElement("button");
        button_to_append.textContent = room_list[i];
        button_to_append.onclick = function() {enterRoom(room_list[i]);}; //ripped straight from lab8/9

        //append to the tr and td elements
        cell.appendChild(button_to_append);
        row.appendChild(cell);
        tableRef.appendChild(row);
    }
}

function enterRoom(code){

    // refresh the list of rooms //TODO: jacob here: I assume we need to have an active list of rooms

    // create the web socket
    ws = new WebSocket("ws://localhost:8080/WSChatServer-1.0-SNAPSHOT/ws/"+code);
    console.log("room code: "+code)

    ws.onmessage = function (event) {
        console.log(event.data);
        let message = JSON.parse(event.data);

        addTable(true,"[" + timestamp() + "] " + message.message + "\n")

        document.getElementById("log").value += "[" + timestamp() + "] " + message.message + "\n";
    }

    document.getElementById("input").addEventListener("keyup", function (event) {
        if (event.key === "Enter") {
            let request = {"type":"chat", "msg":event.target.value};
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

function addTable(column,text)
{
    let table = document.getElementById('chart')
    let newRow = document.createElement("tr")

    let cell = document.createElement("td");
    let cell2 = document.createElement("td2");
    if (column === true)
    {
        cell.appendChild(text);

    }
    else if (column === false)
    {
        cell.appendChild(text);

    }
    cell2.appendChild();
    newRow.appendChild(cell)
    newRow.appendChild(cell2)
    table.appendChild(newRow);

}





