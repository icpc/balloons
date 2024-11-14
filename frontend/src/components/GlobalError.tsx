interface GlobalErrorProps {
  title: string
  message: string
}

export function GlobalError({ title, message }: GlobalErrorProps) {
  return (
    <div className="global-error">
      <h1>{title}</h1>
      <p>{message}</p>
    </div>
  );
}
