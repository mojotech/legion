#!/usr/bin/env bash

# TODO:
#   - set LEGION_REPO some other way

LEGION_REPO=bunsen

#

set -o errtrace -o nounset -o pipefail

version=0.0.1

declare -A options
declare -A aliases
declare -a arguments
declare -a errors
declare -a argv

option_values="[]"
argument_values="[]"
argv=("$@")
errors=()

base_url="http://localhost:3000/api/v1"

local_schema='
  {
    "default": {
      "arguments": [
        {
          "name": "command",
          "description": "a command name"
        },
        {
          "name": "arguments",
          "description": "arguments for the command",
          "multiple": true,
          "optional": true
        }
      ],
      "commands": [
        {
          "name": "help",
          "description": "display the help"
        },
        {
          "name": "version",
          "description": "display the version"
        }
      ]
    },
    "common": {
      "options": [
        {
          "name": "--help",
          "type": "boolean",
          "alias": "-h",
          "description": "display the help"
        },
        {
          "name": "--version",
          "type": "boolean",
          "alias": "-v",
          "description": "display the version"
        }
      ]
    }
  }
  '

remote_schema=''

j='
  def with_options(c): .options |= . + c.options;

  def with_arguments(c): .arguments |= . + c.arguments;

  def command_with_common(n):
    .[0].common as $c | .[1][] | select(.name == n) | with_options($c) | with_arguments($c);

  def commands_with_common:
    .[0] as $l | .[1][] | [with_options($l.common) | with_arguments($l.common)] | . + $l.default.commands;

  def default_with_common:
    .common as $c | .default | with_options($c) | with_arguments($c);

  def extract_options: .options[] | "\(.name) \(.type)";

  def extract_aliases: .options[] | select(has("alias")) | "\(.name) \(.alias)";

  def extract_arguments:
    .arguments[] |
      "\(.name)\(if .optional? and .multiple?
                  then "*"
                  else (if .multiple?
                        then "+"
                        else (if .optional?
                              then "?"
                              else "$"
                              end)
                        end)
                  end)";

  def initialize_option_values:
    [
      .options[] |
        {
          "name": .name,
          "value": null
        }
    ];

  def initialize_argument_values:
    [
      .arguments[] |
        {
          "name": .name,
          "value": (if .multiple?
                    then []
                    else null
                    end)
        }
    ];

  def format_usage:
    .arguments[] |
      "\(if .optional? and .multiple?
        then "[<\(.name)>...]"
        else (if .multiple?
              then "<\(.name)>..."
              else (if .optional?
                    then "[<\(.name)>]"
                    else "<\(.name)>"
                    end)
              end)
        end)";

  def format_options:
    .options[] |
      "\(if .alias?
         then "\(.alias), \(.name)"
         else .name
         end)\t\(.description)";

  def format_arguments:
    .arguments[] |
      "\(.name)\t\(.description)";

  def format_commands:
    .[] |
      "\(.name)\t\(.description)";

  def select_value(n):
    (
      .[] |
        select(.name == n) | .value
    );

  def build_request(n):
    {
      "type": n,
      "options": .[0],
      "arguments": .[1]
    };
    '

function schema {
  if [[ -z $remote_schema ]]; then
    remote_schema=$(get repo/bunsen/command)
  fi

  if [[ -z ${1-} ]]; then
    echo "$local_schema" | jq -c -r "$j default_with_common"
  elif [[ $1 == '*' ]]; then
    echo "$local_schema $remote_schema" | jq -s -c -r "$j commands_with_common"
  else
    echo "$local_schema $remote_schema" | jq -s -c -r --arg name "$1" "$j command_with_common(\$name)"
  fi
}

function parse_schema {
  local schema="$(schema "${1-}")"

  while read -r long type; do
    options["$long"]="$type"
  done <<<"$(echo "$schema" | jq -r -c "$j extract_options")"

  while read -r long short; do
    aliases["$short"]="$long"
  done <<<"$(echo "$schema" | jq -r -c "$j extract_aliases")"

  arguments=($(echo "$schema" | jq -c -r "$j extract_arguments"))

  option_values="$(echo "$schema" | jq -c "$j initialize_option_values")"
  argument_values="$(echo "$schema" | jq -c "$j initialize_argument_values")"
}

function format_columns {
  awk -F"\t" '{printf "    %-20s %-40s\n", $1, $2}'
}

function print_usage {
  echo -n "  $1 "
  jq -r -c "$j format_usage" | paste -sd " " -
}

function print_options {
  echo "  $1"
  jq -r -c "$j format_options" | format_columns
}

function print_arguments {
  echo "  $1"
  jq -r -c "$j format_arguments" | format_columns
}

function print_commands {
  echo "  $1"
  jq -r -c "$j format_commands" | format_columns
}

function print_errors {
  if [[ ${#errors[@]} > 0 ]]; then
    echo "  $(tput setaf 1)$1"
    for e in "${errors[@]}"; do
      echo "    $e"
    done
    echo "$(tput sgr0)"
  fi
}

function print_summary {
  if [[ -n ${1-} ]]; then
    echo
    schema "$1" | print_usage "usage: legion [options] $1 [options]"
    echo
    schema "$1" | print_options "options:"
    echo
    schema "$1" | print_arguments "arguments:"
    echo
    print_errors "errors:"
  else
    echo
    schema | print_usage "usage: legion [options]"
    echo
    schema '*' | print_commands "commands:"
    echo
    schema | print_options "options:"
    echo
    schema | print_arguments "arguments:"
    echo
    print_errors "errors:"
  fi
}

function print_version {
  echo "$version"
}

function select_value {
  local value="$(jq -c -r --arg name "$1" "$j select_value(\$name)")"

  case "${2-string}" in
    (boolean)
      if [[ $value == 'false' || $value == 'null' ]]; then
        echo ""
      else
        echo "$value"
      fi
      ;;
    (*)
      echo "$value"
      ;;
  esac
}

function select_option_value {
  echo "$option_values" | select_value "$@"
}

function select_argument_value {
  echo "$argument_values" | select_value "$@"
}

function update_value {
  local value=''

  case "${3-string}" in
    (null)
      value="null"
      ;;
    (boolean)
      if [[ -n $2 ]]; then
        value="true"
      else
        value="false"
      fi
      ;;
    (*)
      value="\"$2\""
      ;;
  esac

  if [[ ${4-replace} == "append" ]]; then
    jq -c --arg name "$1" "$j select_value(\$name) |= . + [$value]"
  else
    jq -c --arg name "$1" "$j select_value(\$name) |= $value"
  fi
}

function update_option_value {
  option_values="$(echo "$option_values" | update_value "$@")"
}

function update_argument_value {
  argument_values="$(echo "$argument_values" | update_value "$@")"
}

function parse_options {
  local a
  local n
  local t
  local v

  set -- "${argv[@]-}"

  while [[ $# > 0 ]]; do
    case "$1" in
      (--)
        shift
        break
        ;;
      (-?)
        a="${aliases[$1]-}"

        if [[ -n $a ]]; then
          n="$a"
        else
          n="$1"
        fi

        t="${options[$n]-}"

        if [[ -n $t ]]; then
          case $t in
            (string)
              update_option_value "$n" "$2"
              shift
              ;;
            (boolean)
              update_option_value "$n" true boolean
              ;;
            (*)
              errors+=("unknown type: $t")
              ;;
          esac
        else
          errors+=("unknown option: $1")
        fi
        ;;
      (--?*=?*)
        n="${1%=*}"
        v="${1#--*=}"
        t="${options[$n]-}"

        if [[ -n $t ]]; then
          case $t in
            (string)
              update_option_value "$n" "$v"
              ;;
            (boolean)
              errors+=("unexpected value: $n")
              ;;
            (*)
              errors+=("unknown type: $t")
              ;;
          esac
        else
          errors+=("unknown option: $1")
        fi
        ;;
      (--?*)
        t="${options[$1]-}"

        if [[ -n $t ]]; then
          case $t in
            (string)
              errors+=("expected value: $1")
              ;;
            (boolean)
              update_option_value "$1" true boolean
              ;;
            (*)
              errors+=("unknown type: $t")
              ;;
          esac
        else
          errors+=("unknown option: $1")
        fi
        ;;
      (*)
        break
        ;;
    esac
    shift
  done

  argv=("$@")
}

function parse_arguments {
  set -- "${argv[@]-}"

  for a in "${arguments[@]}"; do
    case "$a" in
      (*\?)
        if [[ $# > 0 ]]; then
          update_argument_value "${a%\?}" "$1"
          shift
        fi
        ;;
      (*\+)
        if [[ $# > 0 ]]; then
          update_argument_value "${a%\+}" "$1" "string" "append"
          shift
          while [[ $# > 0 ]]; do
            update_argument_value "${a%\+}" "$1" "string" "append"
            shift
          done
        else
          errors+=("missing argument: ${a%\+}")
        fi
        ;;
      (*\*)
        while [[ $# > 0 ]]; do
          update_argument_value "${a%\*}" "$1" "string" "append"
          shift
        done
        ;;
      (*\$)
        if [[ $# > 0 ]]; then
          update_argument_value "${a%\$}" "$1"
          shift
        else
          errors+=("missing argument: ${a%\$}")
        fi
        ;;
    esac
  done

  while [[ $# > 0 ]]; do
    errors+=("unexpected argument: $1")
    shift
  done

  argv=("$@")
}


function get {
  curl --silent -G -g "$base_url/${1// /%20}"
}

function post {
  curl --silent -d "$2" -X POST -H "Content-Type: application/json" "$base_url/${1// /%20}"
}


function send_request {
  post repo/$LEGION_REPO/commit/HEAD/task "$(echo "$option_values $argument_values" | jq -s -r -c --arg command "$command" "$j build_request(\$command)")"
}

function receive_response {
  :
}

if [[ $# == 0 ]]; then
  print_summary
else
  parse_schema
  parse_options

  if [[ -n $(select_option_value "--help" boolean) ]]; then
    print_summary
  elif [[ -n $(select_option_value "--version" boolean) ]]; then
    print_version
  else
    parse_arguments

    if [[ ${#errors[@]-0} > 0 ]]; then
      print_summary
    else
      command=$(select_argument_value "command")

      if [[ $command == 'help' ]]; then
        print_summary
      elif [[ $command == 'version' ]]; then
        print_version
      elif [[ -z $(schema "$command") ]]; then
        errors+=("unknown command: $command")
        print_summary
      else
        argv=()

        while read -r arg; do
          argv+=("$arg")
        done <<<"$(select_argument_value "arguments" | jq -c -r '.[]')"

        parse_schema "$command"
        parse_options

        if [[ -n $(select_option_value "--help" boolean) ]]; then
          print_summary "$command"
        elif [[ -n $(select_option_value "--version" boolean) ]]; then
          print_version
        else
          parse_arguments
          if [[ ${#errors[@]-0} > 0 ]]; then
            print_summary "$command"
          else
            send_request
            receive_response
          fi
        fi
      fi
    fi
  fi
fi
