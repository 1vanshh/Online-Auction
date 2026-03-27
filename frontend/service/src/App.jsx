import React, { useState } from 'react';

function App() {
  const [response, setResponse] = useState(null);

  const fetchData = async () => {
    const res = await fetch('/api/auth');
    const data = await res.json();
    setResponse(data);
  };

  return (
    <div>
      <button onClick={fetchData}>
        Запрос к /api/auth
      </button>

      {response && (
        <pre>
          {JSON.stringify(response, null, 2)}
        </pre>
      )}
    </div>
  );
}

export default App;
