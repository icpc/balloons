# Balloons Manager

## Requirements

* Java 17

## Setup

Use the same configs as in [ICPC Live Overlay v3](https://github.com/icpc/live-v3). That's all!

## Launch

```bash
java -jar balloons.jar -c path/to/config
```

You can customize a few options:

* All customization supported by [Overlay](https://github.com/icpc/live-v3) are supported!
  You likely want to [set problem colors](https://github.com/icpc/live-v3/blob/main/docs/advanced.json.md#change-problem-info) and
  add custom fields — it's `custom-fields.csv` file in config directory that contain columns `team_id,hall,place`.

* By default, port is 8001, but you can set another one via `--port=1234`.

* By default, registration is enabled (but you still need to approve everyone), you can block self-registration using `--disable-registration`.

Then navigate in your browser to `http://{ip}:{port}/` (by default and from the same machine, http://localhost:8001/). If this service
is exposed to the internet, it's strictly recommended to use some reverse proxy like nginx.

Don't forget to add admin user (see below).

> We create H2 database that contains a few files. They are created in config directory and start with `h2`, e.g. `h2.trace.db`.
> [Read more](http://www.h2database.com/html/features.html#database_file_layout) about database files.
>  
> You may want to `.gitignore` them if you're committing configs to some repository.

## CLI

```bash
# Create a volunteer
java -jar balloons.jar -c config volunteer create login password

# Create an admin
java -jar balloons.jar -c config volunteer create --admin login password

# Make the volunteer an admin
java -jar balloons.jar -c config volunteer update login --make-admin

# Change password
java -jar balloons.jar -c config volunteer update login --new-password=password

# Database SQL shell
java -jar balloons.jar -c config h2shell
```

> [!IMPORTANT]
> When specifying flags, make sure that `-c` / `--config-directory` is set **before** command (`h2shell` / `volunteer`) and other options
> (username, password) are set **after** command.

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

## License

You may use this repository code according to [The MIT License](LICENSE) terms.

## Credits

This project is inspired by previous versions — [neerc/balloons](https://github.com/neerc/balloons) and [nikkirche/balloons-reborn](https://github.com/nikkirche/balloons-reborn).

All of this wouldn't work without [ICPC Live tools](https://github.com/icpc/live-v3).

## TODO

- [ ] Frontend: implement some pings to detect connectivity issues (??)
- [ ] Store time of run and delivery and show it in interface
- [ ] Show number of balloons remaining
- [ ] Tests
- [ ] Some docs on how to develop it
- [ ] i18n
