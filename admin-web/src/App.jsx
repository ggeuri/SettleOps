import { useEffect, useState } from "react";

function App() {
  const [msg, setMsg] = useState("뿌엥");

  useEffect(() => {
    fetch("http://localhost:8080/api/health", { credentials: "include" })
      .then((r) => r.text())
      .then(setMsg("연결돼따리!!!!!!!!"))
      .catch(() => setMsg("서버랑 연결실패해따리..."));
  }, []);

  return <div>프론트 돼따링!!!!!!!!!!!!!!!!!!!!! {msg}</div>;
}

export default App;