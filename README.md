# Balloons Manager

## Requirements

* Java 17

## Setup

1. Use the same configs as in [ICPC Live Overlay v3](https://github.com/icpc/live-v3)
2. Add `balloons.json` with

   ```json
   {
     "secretKey": "",                 // (required)
     "allowPublicRegistration": true, // (default: true)
     "port": 8000                     // (default: 8001)
   }
   ```

   If public registration is allowed (**default**), users can create themselves but you still need to approve them.
   Otherwise, you need to register everyone in admin interface or via CLI.

## Launch

```bash
java -jar balloons.jar run -c path/to/config
```

You can customize a few options:
* We create H2 database that contains a few files. You can control its location by `--database-file=/path/to/h2`, by default it is created
  in current working directory with prefix `h2`. [Read more](http://www.h2database.com/html/features.html#database_file_layout) about database files.

* All customization supported by [Overlay](https://github.com/icpc/live-v3) are supported!
  You likely want to [set problem colors](https://github.com/icpc/live-v3/blob/main/docs/advanced.json.md#change-problem-info).

> [!IMPORTANT] 
> When specifying flags, make sure that `-d` / `--database-file` is set **before** command (`run` / `volunteer`) and all other options
> are set **after** command.

Then navigate in your browser to `http://{ip}:{port}/` (by default and from the same machine, http://localhost:8001/). If this service
is exposed to the internet, it's strictly recommended to use some reverse proxy like nginx.

Don't forget to add admin user (see below).

## CLI

```bash
# Create a volunteer
java -jar balloons.jar volunteer create login password

# Create an admin
java -jar balloons.jar volunteer create --admin login password

# Make the volunteer an admin
java -jar balloons.jar volunteer update login --make-admin

# Change password
java -jar balloons.jar volunteer update login --new-password=password

# Database SQL shell
java -jar balloons.jar h2shell
```

## Development

### Frontend

You can launch frontend in development mode:

```bash
cd frontend/
pnpm run dev
```

It will start at port 5173. To avoid CORS-tricks, it proxies `/api/` to `http://localhost:8001`. If backend is located somewhere else,
set `BACKEND_URL` environment variable. See [vite.config.ts](frontend/vite.config.ts) for details.

### Build

This task should do the trick.

```bash
gradle shadowJar
```

## TODO

- [ ] Frontend: detect connection errors
- [ ] Tests
- [ ] Some docs on how to develop it
- [ ] Automate JWT secret generation 
